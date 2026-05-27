package com.ecommerce.service;

import com.ecommerce.dto.OrderCreatedEvent;
import com.ecommerce.dto.OrderItemResponse;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.dto.PlaceOrderRequest;
import com.ecommerce.entity.*;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.messaging.OrderEventPublisher;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * OrderService.java — Order creation with async RabbitMQ flow
 * ============================================================
 *
 * HOW THE NEW ORDER FLOW WORKS:
 *
 *   OLD synchronous flow (Phase 2):
 *     User POSTs /api/orders/place
 *       → Stock checked and reduced immediately
 *       → Order saved as CONFIRMED
 *       → Cart cleared
 *       → Response returned (user waits for ALL of this!)
 *
 *   NEW asynchronous flow (Phase 3 — this file):
 *     User POSTs /api/orders/place
 *       → Order created with status = PENDING
 *       → Order items copied from cart (snapshot)
 *       → Order saved to database
 *       → OrderCreatedEvent published to RabbitMQ
 *       → Response returned IMMEDIATELY to user ("Order is being processed")
 *            ↓ (background — user does NOT wait for this)
 *       RabbitMQ delivers event to OrderEventConsumer
 *       → Stock checked with pessimistic lock
 *       → If OK: stock reduced, order → CONFIRMED, cart cleared
 *       → If not OK: order → FAILED, nothing touched
 *
 * WHY IS THIS BETTER?
 *   1. FASTER user experience: User gets an instant response.
 *   2. RESILIENT: If the processing fails, the order is FAILED
 *      and the user still has their cart intact to retry.
 *   3. SCALABLE: Multiple consumer instances can process orders
 *      in parallel from the same queue.
 *   4. DECOUPLED: OrderService doesn't care HOW the order is
 *      processed — it just fires the event and moves on.
 *
 * IMPORTANT CHANGES FROM PHASE 2:
 *   - We NO LONGER reduce stock inside placeOrder.
 *   - We NO LONGER clear the cart inside placeOrder.
 *   - Both of those now happen inside OrderEventConsumer.
 *   - We NO LONGER use findByIdForUpdate here.
 *   - Order status starts as PENDING (not CONFIRMED).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final OrderEventPublisher orderEventPublisher;

    /**
     * ============================================================
     * placeOrder — Creates order as PENDING and fires a RabbitMQ event
     * ============================================================
     *
     * @Transactional ensures:
     *   - Saving the order and all its items is one atomic operation.
     *   - If anything fails before the event is published (e.g. DB error),
     *     the order is NOT saved and the user gets a clean error.
     *   - We commit the order to DB before publishing the event,
     *     so the consumer can find it by ID when it processes it.
     */
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request, String userEmail) {

        // ─────────────────────────────────────────────────────
        // STEP 1: Validate the logged-in user
        // ─────────────────────────────────────────────────────
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + userEmail));

        // ─────────────────────────────────────────────────────
        // STEP 2: Find the user's cart
        // ─────────────────────────────────────────────────────
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user: " + user.getName()));

        // ─────────────────────────────────────────────────────
        // STEP 3: Reject if cart is empty
        // ─────────────────────────────────────────────────────
        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException(
                    "Cart is empty. Add products before placing an order.");
        }

        // ─────────────────────────────────────────────────────
        // STEP 4: Build the Order with status = PENDING
        //
        // WHY PENDING?
        //   The order hasn't been processed yet.
        //   Stock hasn't been checked or reduced.
        //   The order event is about to be sent to RabbitMQ.
        //   PENDING means: "Created, waiting to be processed."
        // ─────────────────────────────────────────────────────
        Order order = Order.builder()
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .phoneNumber(request.getPhoneNumber())
                .status(OrderStatus.PENDING)        // ← Key change: PENDING, not CONFIRMED
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .build();

        // ─────────────────────────────────────────────────────
        // STEP 5: Copy cart items into order items (snapshot)
        //
        // WHY DO WE COPY CART ITEMS INTO ORDER ITEMS?
        //   We take a "snapshot" of the cart at order time.
        //   This preserves the product name, price, and image URL
        //   even if the product is later updated or deleted.
        //   So order history always shows what the user actually bought.
        //
        // NOTE: We do NOT reduce stock here. That happens in the consumer.
        // NOTE: We do NOT clear the cart here. That happens after CONFIRMED.
        // ─────────────────────────────────────────────────────
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();

            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())          // snapshot
                    .productImageUrl(product.getImageUrl())  // snapshot
                    .price(product.getPrice())               // snapshot
                    .quantity(cartItem.getQuantity())
                    .subtotal(subtotal)
                    .build();

            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);

        // ─────────────────────────────────────────────────────
        // STEP 6: Calculate total amount
        // ─────────────────────────────────────────────────────
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // ─────────────────────────────────────────────────────
        // STEP 7: Save the order to MySQL
        //
        // IMPORTANT: We MUST save the order BEFORE publishing the event.
        // The consumer will try to fetch the order by orderId.
        // If we haven't saved it yet, the consumer won't find it!
        // ─────────────────────────────────────────────────────
        Order savedOrder = orderRepository.save(order);
        log.info("Order {} created with status PENDING for user: {}", savedOrder.getId(), userEmail);

        // ─────────────────────────────────────────────────────
        // STEP 8: Publish the OrderCreatedEvent to RabbitMQ
        //
        // This is like dropping a letter in the post office.
        // The letter (event) contains the orderId and userId.
        // The post office (RabbitMQ) will deliver it to the consumer.
        // The consumer will then do the actual stock checking + reduction.
        //
        // AFTER this line returns, the user gets their response.
        // The consumer processes the event in the background.
        // ─────────────────────────────────────────────────────
        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .userId(user.getId())
                .createdAt(LocalDateTime.now())
                .build();

        orderEventPublisher.publishOrderCreatedEvent(event);

        // ─────────────────────────────────────────────────────
        // STEP 9: Return the OrderResponse with PENDING status
        // ─────────────────────────────────────────────────────
        return mapToOrderResponse(savedOrder);
    }

    /**
     * Returns all orders for the logged-in user, newest first.
     * After async processing, orders here will show CONFIRMED or FAILED.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + userEmail));

        List<Order> orders = orderRepository.findByUserOrderByOrderDateDesc(user);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Returns a specific order by ID for the logged-in user.
     * Only returns the order if it belongs to the logged-in user.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + userEmail));

        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + orderId));

        return mapToOrderResponse(order);
    }

    /**
     * Converts an Order entity into an OrderResponse DTO.
     */
    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .orderItemId(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProductName())
                        .productImageUrl(item.getProductImageUrl())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        int totalItems = order.getOrderItems().stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUser().getId())
                .userName(order.getUser().getName())
                .items(itemResponses)
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())   // Will show PENDING initially
                .shippingAddress(order.getShippingAddress())
                .phoneNumber(order.getPhoneNumber())
                .orderDate(order.getOrderDate())
                .totalItems(totalItems)
                .build();
    }
}
