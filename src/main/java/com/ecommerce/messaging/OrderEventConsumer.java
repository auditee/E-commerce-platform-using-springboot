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

import java.math.BigDecimal;

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
            // This should not happen normally, but we log it just in case.
            log.error("Order not found for orderId: {}. Skipping processing.", event.getOrderId());
            return;
        }

        // ─────────────────────────────────────────────────────
        // STEP 2: Duplicate processing protection
        //
        // WHY IS THIS NEEDED?
        //   In rare cases, RabbitMQ may deliver the same message
        //   twice (e.g. if the consumer crashes right after processing
        //   but before acknowledging the message).
        //   If we processed an already-CONFIRMED order again, we'd
        //   reduce stock a second time — causing negative stock!
        //
        //   This check ensures we only process PENDING orders.
        //   If the order is already CONFIRMED or FAILED, we skip it.
        // ─────────────────────────────────────────────────────
        if (!order.getStatus().equals(OrderStatus.PENDING)) {
            log.info("Order {} is already in status '{}'. Skipping duplicate processing.",
                    order.getId(), order.getStatus());
            return;
        }

        log.info("Starting stock validation for orderId: {} with {} items.",
                order.getId(), order.getOrderItems().size());

        // ─────────────────────────────────────────────────────
        // STEP 3: Stock validation with pessimistic locking
        //
        // WHY PESSIMISTIC LOCKING HERE?
        //   The consumer runs in a background thread.
        //   If two orders for the same product are processed
        //   at the same time, they could both pass the stock check
        //   and both reduce stock — causing negative stock!
        //
        //   findByIdForUpdate() runs: SELECT * FROM products WHERE id = ? FOR UPDATE
        //   This LOCKS the product row. Only one consumer thread can
        //   hold the lock at a time. The other must wait.
        //   This is the same protection we had in the synchronous flow.
        // ─────────────────────────────────────────────────────
        for (OrderItem item : order.getOrderItems()) {
            Long productId = item.getProduct().getId();

            Product product = productRepository.findByIdForUpdate(productId).orElse(null);

            if (product == null) {
                log.error("Product with id {} not found during order {} processing. Marking as FAILED.",
                        productId, order.getId());
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
                return;
            }

            // Check if stock is enough for this item
            if (product.getStockQuantity() < item.getQuantity()) {
                log.warn("Insufficient stock for product '{}' (id: {}). Required: {}, Available: {}. Marking order {} as FAILED.",
                        product.getName(), productId, item.getQuantity(),
                        product.getStockQuantity(), order.getId());

                // Mark order as FAILED — do NOT reduce stock, do NOT clear cart
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);

                log.info("Order {} marked as FAILED due to insufficient stock.", order.getId());
                return; // Stop processing — don't touch any other products
            }
        }

        // ─────────────────────────────────────────────────────
        // STEP 4: All stock checks passed — reduce stock
        //
        // We only reach here if EVERY item has enough stock.
        // Now we do the actual reductions in a second pass.
        //
        // WHY TWO PASSES (check first, then reduce)?
        //   If we reduced stock item by item and then found an
        //   insufficient item midway through, we'd have partially
        //   reduced stock — an inconsistent state!
        //   By checking ALL items first, we ensure we only reduce
        //   stock when we're 100% sure the whole order can succeed.
        // ─────────────────────────────────────────────────────
        log.info("All stock checks passed for orderId: {}. Reducing stock.", order.getId());

        for (OrderItem item : order.getOrderItems()) {
            Long productId = item.getProduct().getId();

            // Fetch with lock again for the actual update
            Product product = productRepository.findByIdForUpdate(productId)
                    .orElseThrow(() -> new RuntimeException(
                            "Product disappeared during processing: " + productId));

            int newStock = product.getStockQuantity() - item.getQuantity();
            product.setStockQuantity(newStock);
            productRepository.save(product);

            log.info("Reduced stock for product '{}' (id: {}) by {}. New stock: {}.",
                    product.getName(), productId, item.getQuantity(), newStock);
        }

        // ─────────────────────────────────────────────────────
        // STEP 5: Mark order as CONFIRMED
        // ─────────────────────────────────────────────────────
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Order {} successfully CONFIRMED.", order.getId());

        // ─────────────────────────────────────────────────────
        // STEP 6: Clear the user's cart
        //
        // WHY CLEAR CART ONLY HERE (AFTER CONFIRMED)?
        //   If we cleared the cart in OrderService (during placeOrder),
        //   and then the consumer marks the order FAILED, the user's
        //   cart would already be gone — they'd lose their items!
        //   By clearing ONLY after CONFIRMED, the cart remains intact
        //   if the order fails, and the user can retry.
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
