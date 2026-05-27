package com.ecommerce.service;

import com.ecommerce.dto.OrderItemResponse;
import com.ecommerce.dto.OrderResponse;
import com.ecommerce.dto.PlaceOrderRequest;
import com.ecommerce.entity.*;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * OrderService.java — The Order Business Logic Brain
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It contains all core business logic related to placing and viewing orders.
 *
 * TRANSACTION MANAGEMENT:
 *   We annotate placeOrder with @Transactional.
 *   If any step fails (e.g. out of stock, database down), the entire process 
 *   is rolled back automatically (product stock is not reduced, and the cart 
 *   is not cleared).
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    /**
     * Places an order from the user's shopping cart.
     * Annotated with @Transactional for safe database rollbacks.
     */
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request, String userEmail) {
        // Step 1: Find the logged-in user by email
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // Step 2: Find the user's cart
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + user.getName()));

        // Step 3 & 4: Check if the cart has items. If empty, throw an error
        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException("Cart is empty. Add products before placing an order.");
        }

        // Step 6: Create a new Order entity
        Order order = Order.builder()
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .phoneNumber(request.getPhoneNumber())
                .status(OrderStatus.CONFIRMED) // Automatically CONFIRMED on placement
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();

        // Step 5 & 7: Check stock, create order items, reduce stock for every cart item
        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            
            // Check available stock
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("Product " + product.getName() + " has only " + product.getStockQuantity() + " items available in stock");
            }

            // Reduce product stock by ordered quantity
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            // Copy product snapshot (name, image, price) into OrderItem to preserve history
            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .productName(product.getName())
                    .productImageUrl(product.getImageUrl())
                    .price(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(subtotal)
                    .build();

            orderItems.add(orderItem);
        }

        order.setOrderItems(orderItems);

        // Step 8: Calculate total order amount as sum of all order item subtotals
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // Step 11: Save order with order items (cascade = CascadeType.ALL handles order items)
        Order savedOrder = orderRepository.save(order);

        // Step 12: Clear the cart after successful order placement
        cartService.clearCart(cart);

        // Step 13: Return OrderResponse
        return mapToOrderResponse(savedOrder);
    }

    /**
     * Fetches all orders belonging to the logged-in user, sorted newest first.
     */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        List<Order> orders = orderRepository.findByUserOrderByOrderDateDesc(user);
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    /**
     * Fetches a specific order by ID, ensuring it belongs to the logged-in user.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        // findByIdAndUser guarantees that the user can only view their own order
        Order order = orderRepository.findByIdAndUser(orderId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        return mapToOrderResponse(order);
    }

    /**
     * Helper method to map an Order entity to an OrderResponse DTO.
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
                .status(order.getStatus().name())
                .shippingAddress(order.getShippingAddress())
                .phoneNumber(order.getPhoneNumber())
                .orderDate(order.getOrderDate())
                .totalItems(totalItems)
                .build();
    }
}
