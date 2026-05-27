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
 * OrderService.java — Order Business Logic with Database Locking
 * ============================================================
 *
 * WHAT IS A RACE CONDITION?
 *   A race condition happens when two or more requests run at the
 *   same time and interfere with each other in unexpected ways.
 *
 *   Real-world example with e-commerce stock:
 *
 *     Product stock = 1 (only one item left)
 *
 *     Time 00:00 → User A reads stock = 1 → "OK, enough stock"
 *     Time 00:00 → User B reads stock = 1 → "OK, enough stock"  ← SAME TIME!
 *     Time 00:01 → User A reduces stock → stock = 0, order saved ✅
 *     Time 00:01 → User B reduces stock → stock = -1 ← NEGATIVE! BUG! ❌
 *
 *   Both users passed the stock check because they read the same
 *   value before either one had a chance to reduce it.
 *   This is the race condition. The "race" is between two requests
 *   for the same data at the same time.
 *
 * WHAT IS PESSIMISTIC LOCKING?
 *   Pessimistic locking means: "I assume someone else WILL try to
 *   change this data at the same time, so I'll lock it just in case."
 *
 *   When we call findByIdForUpdate(), MySQL runs:
 *     SELECT * FROM products WHERE id = ? FOR UPDATE
 *
 *   "FOR UPDATE" puts an exclusive lock on that product row.
 *   This means:
 *     - The current transaction can read and update the row.
 *     - ALL other transactions that try to read that row are BLOCKED.
 *     - They must WAIT until our transaction finishes (commit/rollback).
 *
 *   With locking:
 *     Time 00:00 → User A reads stock = 1 → MySQL LOCKS the product row.
 *     Time 00:00 → User B tries to read same row → BLOCKED. Must wait.
 *     Time 00:01 → User A reduces stock → stock = 0 → commits → UNLOCK.
 *     Time 00:01 → User B is unblocked → reads stock = 0 → check FAILS.
 *     Time 00:01 → User B order REJECTED with "out of stock" message. ✅
 *
 *   Stock never goes negative. Only one order succeeds. 
 *
 * WHY IS @Transactional REQUIRED?
 *   Pessimistic locks ONLY work inside a database transaction.
 *   A transaction is a group of database operations treated as ONE unit:
 *     - Either ALL succeed (commit), or
 *     - ALL are undone (rollback).
 *
 *   Without @Transactional:
 *     - There is no transaction context.
 *     - The lock acquired by findByIdForUpdate() is immediately released.
 *     - The lock provides ZERO protection.
 *
 *   With @Transactional:
 *     - Spring opens a transaction before the method starts.
 *     - The lock is held for the ENTIRE method duration.
 *     - If any step fails, ALL database changes are rolled back:
 *         - Stock is NOT reduced
 *         - Order is NOT saved
 *         - Cart is NOT cleared
 *     - Spring commits and releases the lock when the method ends.
 *
 * WHY IS STOCK REDUCED ONLY DURING ORDER PLACEMENT (NOT CART ADD)?
 *   Adding a product to cart does NOT mean the user will buy it.
 *   The user might:
 *     - Add to cart, then abandon the cart.
 *     - Change their mind and remove items.
 *     - Just be browsing.
 *
 *   If we reduced stock when items are added to cart, stock could
 *   reach 0 even when no real purchases happened.
 *   We only reduce stock when the user actually places an order.
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
     * ============================================================
     * placeOrder — Places an order safely using database locking
     * ============================================================
     *
     * @Transactional is REQUIRED here because:
     *   1. It opens a database transaction at the start of this method.
     *   2. findByIdForUpdate() acquires a PESSIMISTIC_WRITE lock that
     *      is held for the entire transaction duration.
     *   3. If any step fails (stock insufficient, database error, etc.),
     *      Spring automatically rolls back ALL changes:
     *        - Product stock is restored to its original value.
     *        - Order record is NOT saved to the database.
     *        - Cart is NOT cleared.
     *   4. When the method finishes successfully, Spring commits all
     *      changes and releases the lock.
     */
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request, String userEmail) {

        // ─────────────────────────────────────────────────────
        // STEP 1: Find the logged-in user by their JWT email
        // ─────────────────────────────────────────────────────
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + userEmail));

        // ─────────────────────────────────────────────────────
        // STEP 2: Find the user's shopping cart
        // ─────────────────────────────────────────────────────
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user: " + user.getName()));

        // ─────────────────────────────────────────────────────
        // STEP 3: Reject order if cart is empty
        // ─────────────────────────────────────────────────────
        if (cart.getCartItems().isEmpty()) {
            throw new RuntimeException(
                    "Cart is empty. Add products before placing an order.");
        }

        // ─────────────────────────────────────────────────────
        // STEP 4: Build the Order shell (no items yet)
        // ─────────────────────────────────────────────────────
        Order order = Order.builder()
                .user(user)
                .shippingAddress(request.getShippingAddress())
                .phoneNumber(request.getPhoneNumber())
                .status(OrderStatus.CONFIRMED)
                .orderDate(LocalDateTime.now())
                .totalAmount(BigDecimal.ZERO)
                .orderItems(new ArrayList<>())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();

        // ─────────────────────────────────────────────────────
        // STEP 5: Process each cart item with LOCKING
        // ─────────────────────────────────────────────────────
        for (CartItem cartItem : cart.getCartItems()) {

            Long productId = cartItem.getProduct().getId();

            /**
             * WHY findByIdForUpdate() INSTEAD OF findById()?
             *
             * Regular findById() just reads the product without any lock:
             *   SELECT * FROM products WHERE id = ?
             *   → Anyone can read this row simultaneously. Race condition!
             *
             * findByIdForUpdate() reads the product AND locks the row:
             *   SELECT * FROM products WHERE id = ? FOR UPDATE
             *   → No other transaction can touch this row until we finish.
             *
             * This is the KEY change that prevents stock overselling.
             * The lock is held until this entire @Transactional method ends.
             *
             * If two requests call placeOrder() at the same time for the
             * same product, one will get the lock first and proceed,
             * while the other waits. When the first completes and the
             * second gets the lock, it reads the UPDATED stock value.
             */
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Product not found with id: " + productId));

            /**
             * STOCK VALIDATION (after acquiring the lock):
             *
             * Now we read the REAL current stock value — the one that
             * reflects any changes made by other transactions that
             * committed just before us.
             *
             * If stock is insufficient, we throw a RuntimeException.
             * @Transactional catches this and rolls back everything:
             *   - No stock is reduced
             *   - No order is saved
             *   - Cart remains unchanged
             * The user sees a clean "out of stock" error message.
             */
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException(
                        "Product \"" + product.getName() + "\" has only "
                        + product.getStockQuantity()
                        + " items available in stock");
            }

            // ─────────────────────────────────────────
            // Reduce stock (safe because we hold the lock)
            // ─────────────────────────────────────────
            /**
             * WHY IS THIS SAFE NOW?
             *   Because we hold a PESSIMISTIC_WRITE lock on this product row.
             *   No other transaction can have read or modified this row
             *   since we acquired the lock. We are the only one.
             *   The value we read is guaranteed to be the latest value.
             *   Reducing it here is safe and accurate.
             */
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            // Build the OrderItem (snapshot of product details at purchase time)
            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

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

        // ─────────────────────────────────────────────────────
        // STEP 6: Attach items and calculate order total
        // ─────────────────────────────────────────────────────
        order.setOrderItems(orderItems);

        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(totalAmount);

        // ─────────────────────────────────────────────────────
        // STEP 7: Save the Order (CascadeType.ALL saves OrderItems too)
        // ─────────────────────────────────────────────────────
        Order savedOrder = orderRepository.save(order);

        // ─────────────────────────────────────────────────────
        // STEP 8: Clear the cart AFTER successful order placement
        // ─────────────────────────────────────────────────────
        /**
         * WHY DO WE CLEAR CART ONLY HERE (AT THE END)?
         *   If any earlier step failed (e.g. out of stock), an exception
         *   was already thrown, and @Transactional rolled back everything.
         *   We never reach this line in a failure scenario.
         *   So cart is only cleared when ALL steps completed successfully.
         */
        cartService.clearCart(cart);

        // ─────────────────────────────────────────────────────
        // STEP 9: Return the OrderResponse DTO
        // ─────────────────────────────────────────────────────
        return mapToOrderResponse(savedOrder);
    }

    /**
     * Fetches all orders belonging to the logged-in user, sorted newest first.
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
     * Fetches a specific order by ID, ensuring it belongs to the logged-in user.
     * findByIdAndUser guarantees users can only view their OWN orders.
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
     * Helper method to convert an Order entity into an OrderResponse DTO.
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
