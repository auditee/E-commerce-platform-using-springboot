package com.ecommerce.messaging;

import com.ecommerce.dto.OrderCreatedEvent;
import com.ecommerce.entity.*;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ============================================================
 * OrderEventConsumer.java — Processes orders from RabbitMQ
 * ============================================================
 *
 * WHAT IS A CONSUMER?
 *   A consumer is the part of the app that RECEIVES and PROCESSES
 *   messages from a RabbitMQ queue.
 *
 *   Think of it like this:
 *     Publisher    = Person who drops a letter in the mailbox.
 *     RabbitMQ     = The post office.
 *     Consumer     = The recipient who opens and reads the letter,
 *                    then acts on it.
 *
 * WHAT DOES THIS CLASS DO?
 *   This class listens to "order.queue" for new OrderCreatedEvent messages.
 *   When a new message arrives, it:
 *     1. Fetches the full Order from MySQL using the orderId.
 *     2. Checks stock for every item using pessimistic locking.
 *     3. If ALL stock is available:
 *          - Reduces stock for every product.
 *          - Updates order status to CONFIRMED.
 *          - Clears the user's cart.
 *     4. If ANY item has insufficient stock:
 *          - Updates order status to FAILED.
 *          - Does NOT reduce any stock.
 *          - Does NOT clear the cart (user can fix and retry).
 *
 * WHY IS @Transactional REQUIRED HERE?
 *   The consumer does multiple database operations:
 *     - Read order (with all items)
 *     - Lock + read products (pessimistic lock)
 *     - Update product stock
 *     - Update order status
 *     - Clear cart
 *   All of these must succeed or ALL must be rolled back.
 *   @Transactional ensures this "all or nothing" behavior.
 *
 *   Also, pessimistic locking (@Lock PESSIMISTIC_WRITE) only works
 *   inside an active database transaction.
 *
 * ANNOTATIONS:
 *   @Slf4j → Lombok creates a 'log' variable for us.
 *   @Service → Spring manages this as a bean.
 *   @RequiredArgsConstructor → Injects all 'final' fields.
 *   @RabbitListener → Marks this class/method as a RabbitMQ consumer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;

    /**
     * ============================================================
     * processOrderCreatedEvent — The consumer method
     * ============================================================
     *
     * @RabbitListener(queues = "${order.queue}")
     *   This annotation tells Spring:
     *   "Watch the queue named 'order.queue' (from application.properties).
     *    Whenever a new message arrives in that queue, call this method
     *    automatically and pass the message as the 'event' parameter."
     *
     *   Spring + RabbitMQ together:
     *     1. Receive raw JSON bytes from the queue.
     *     2. Use Jackson2JsonMessageConverter to deserialize them back
     *        into an OrderCreatedEvent Java object.
     *     3. Call this method with that object.
     *
     * @Transactional
     *   Opens a database transaction for the entire method.
     *   - Pessimistic locks (findByIdForUpdate) REQUIRE a transaction.
     *   - If ANY step fails, ALL changes are rolled back automatically.
     *   - Stock is only committed if ALL items pass the stock check.
     *
     * @param event The OrderCreatedEvent received from RabbitMQ.
     *              Contains: orderId, userId, createdAt.
     */
    @Transactional
    @RabbitListener(queues = "${order.queue}")
    public void processOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Received order created event from RabbitMQ for orderId: {}", event.getOrderId());

        // ─────────────────────────────────────────────────────
        // STEP 1: Fetch the Order from the database
        // ─────────────────────────────────────────────────────
        Order order = orderRepository.findById(event.getOrderId()).orElse(null);

        if (order == null) {
            log.error("Order not found for orderId: {}. Skipping processing.", event.getOrderId());
            return;
        }

        // ─────────────────────────────────────────────────────
        // STEP 2: Duplicate processing protection
        // ─────────────────────────────────────────────────────
        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            log.info("Order {} is already in status '{}'. Skipping duplicate processing.",
                    order.getId(), order.getStatus());
            return;
        }

        log.info("Starting stock validation for orderId: {} with {} items.",
                order.getId(), order.getOrderItems().size());

        // ─────────────────────────────────────────────────────
        // STEP 3: Sort order items by productId to prevent deadlocks
        // ─────────────────────────────────────────────────────
        List<OrderItem> sortedItems = order.getOrderItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .collect(Collectors.toList());

        // ─────────────────────────────────────────────────────
        // STEP 4: Stock validation with pessimistic locking
        // ─────────────────────────────────────────────────────
        Map<Long, Product> lockedProductsMap = new HashMap<>();

        for (OrderItem item : sortedItems) {
            Long productId = item.getProduct().getId();

            log.info("Attempting to acquire pessimistic write lock for product id: {} (Order: {})", productId, order.getId());
            Product product = productRepository.findByIdForUpdate(productId).orElse(null);

            if (product == null) {
                log.error("Product with id {} not found during order {} processing. Marking as FAILED.",
                        productId, order.getId());
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
                log.info("Order {} status changed to FAILED because product was not found.", order.getId());
                return;
            }

            log.info("Pessimistic write lock successfully acquired for product '{}' (id: {}). Current stock: {}", 
                    product.getName(), productId, product.getStockQuantity());

            // Check if stock is enough for this item
            if (product.getStockQuantity() < item.getQuantity()) {
                log.warn("Insufficient stock for product '{}' (id: {}). Required: {}, Available: {}. Marking order {} as FAILED.",
                        product.getName(), productId, item.getQuantity(),
                        product.getStockQuantity(), order.getId());

                // Mark order as FAILED — do NOT reduce stock, do NOT clear cart
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);

                log.info("Order {} status changed to FAILED due to insufficient stock for product id: {}.", order.getId(), productId);
                return; // Stop processing — don't touch any other products
            }

            // Cache the locked product to reuse in the reduction step
            lockedProductsMap.put(productId, product);
        }

        // ─────────────────────────────────────────────────────
        // STEP 5: All stock checks passed — reduce stock
        // ─────────────────────────────────────────────────────
        log.info("Stock validation successful for orderId: {}. Reducing stock.", order.getId());

        for (OrderItem item : sortedItems) {
            Long productId = item.getProduct().getId();
            Product product = lockedProductsMap.get(productId);

            if (product == null) {
                log.error("Product id {} not found in locked products cache map during reduction. This should not happen.", productId);
                throw new RuntimeException("Product not found in locked products cache map: " + productId);
            }

            int newStock = product.getStockQuantity() - item.getQuantity();
            product.setStockQuantity(newStock);
            productRepository.save(product);

            log.info("Reduced stock for product '{}' (id: {}) by {}. New stock: {}.",
                    product.getName(), productId, item.getQuantity(), newStock);
        }

        // ─────────────────────────────────────────────────────
        // STEP 6: Mark order as CONFIRMED
        // ─────────────────────────────────────────────────────
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Order {} status changed to CONFIRMED.", order.getId());

        // ─────────────────────────────────────────────────────
        // STEP 7: Clear the user's cart
        // ─────────────────────────────────────────────────────
        Cart cart = cartRepository.findByUserId(event.getUserId()).orElse(null);
        if (cart != null) {
            cartService.clearCart(cart);
            log.info("Cart cleared for userId: {} after successful order confirmation.", event.getUserId());
        } else {
            log.warn("Cart not found for userId: {} during post-confirmation cleanup.", event.getUserId());
        }

        log.info("Async order processing completed successfully for orderId: {}", order.getId());
    }
}
