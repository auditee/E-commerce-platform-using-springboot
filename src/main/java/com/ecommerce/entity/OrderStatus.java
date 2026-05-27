package com.ecommerce.entity;

/**
 * ============================================================
 * OrderStatus.java — The Order Status Enum
 * ============================================================
 *
 * WHAT IS AN ENUM?
 *   An enum (enumeration) is a special Java type that defines a
 *   fixed set of named constants. Instead of using random strings
 *   like "confirmed" or "CONFIRMED" or "Confirmed" (which can
 *   cause bugs), we use an enum value that is always consistent.
 *
 * ORDER LIFECYCLE (with RabbitMQ async processing):
 *
 *   1. PENDING
 *      The user has called POST /api/orders/place.
 *      The order has been created and saved in the database.
 *      An event has been published to RabbitMQ.
 *      But processing has NOT started yet.
 *      The order is "waiting in the queue" to be processed.
 *
 *   2. CONFIRMED
 *      The RabbitMQ consumer has processed the order successfully.
 *      Stock was available, it was reduced, and the cart was cleared.
 *      The order is confirmed and ready for shipping.
 *
 *   3. FAILED
 *      The RabbitMQ consumer tried to process the order but failed.
 *      Most common reason: product stock was insufficient.
 *      Stock was NOT reduced. Cart was NOT cleared.
 *      The user can update their cart and try again.
 *
 *   4. CANCELLED
 *      The order was cancelled (by user or admin).
 *      Reserved for future cancellation feature.
 *
 * FLOW DIAGRAM:
 *
 *   POST /api/orders/place
 *          ↓
 *       PENDING   ←── order saved, event published to RabbitMQ
 *          ↓
 *   [RabbitMQ Consumer processes event]
 *          ↓
 *     ┌────────────┐
 *     ↓            ↓
 *  CONFIRMED     FAILED
 *  (stock OK)  (no stock)
 */
public enum OrderStatus {

    /**
     * Order is created but not yet processed.
     * This is the initial state when a user places an order.
     * The order event is sitting in the RabbitMQ queue.
     */
    PENDING,

    /**
     * Order has been successfully processed.
     * Stock was checked and reduced. Cart was cleared.
     * This is the "happy path" final state.
     */
    CONFIRMED,

    /**
     * Order processing failed.
     * Usually means stock was insufficient for one or more items.
     * Stock was NOT reduced. Cart was NOT cleared.
     * The user can fix their cart and try placing a new order.
     */
    FAILED,

    /**
     * Order was cancelled.
     * Reserved for a future cancellation feature.
     */
    CANCELLED
}
