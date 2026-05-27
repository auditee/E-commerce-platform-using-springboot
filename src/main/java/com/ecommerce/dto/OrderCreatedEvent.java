package com.ecommerce.dto;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ============================================================
 * OrderCreatedEvent.java — The "message" sent to RabbitMQ
 * ============================================================
 *
 * WHAT IS AN EVENT?
 *   An event is a simple notification that says "something happened."
 *   In our case, the event is: "A new order was just created."
 *
 *   Think of it like sending a text message to a friend:
 *     You (OrderService) send a text: "Hey, Order #42 was just placed!"
 *     Your friend (OrderEventConsumer) receives the text and acts on it.
 *
 *   The text message here is the OrderCreatedEvent object.
 *
 * WHAT DATA DOES IT CONTAIN?
 *   - orderId  → Which order was created? (e.g. 42)
 *   - userId   → Which user placed it? (e.g. 5)
 *   - createdAt → When was it created? (timestamp for logging)
 *
 * WHY NOT INCLUDE FULL ORDER DETAILS?
 *   We only send the orderId. The consumer will fetch the full
 *   order details from the database when it processes the event.
 *   This keeps the message small and avoids sending stale data.
 *
 * WHY IMPLEMENTS SERIALIZABLE?
 *   RabbitMQ stores messages as bytes. Java must convert (serialize)
 *   this object into bytes to send it. implements Serializable
 *   tells Java that this object is safe to convert to bytes.
 *
 * HOW DOES THIS FLOW WORK?
 *
 *   1. User calls POST /api/orders/place
 *   2. OrderService creates an Order with status = PENDING
 *   3. OrderService builds an OrderCreatedEvent:
 *        { orderId: 42, userId: 5, createdAt: "2026-05-27T19:00:00" }
 *   4. OrderEventPublisher sends this event to RabbitMQ queue
 *   5. OrderEventConsumer receives it and processes the order
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreatedEvent implements Serializable {

    /**
     * The ID of the order that was just created.
     * The consumer uses this to fetch the full order from MySQL.
     */
    private Long orderId;

    /**
     * The ID of the user who placed the order.
     * Used for logging and finding the user's cart to clear it.
     */
    private Long userId;

    /**
     * The timestamp of when this event was created.
     * Useful for debugging, tracing, and logging.
     */
    private LocalDateTime createdAt;
}
