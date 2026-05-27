package com.ecommerce.messaging;

import com.ecommerce.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * ============================================================
 * OrderEventPublisher.java — Sends order events to RabbitMQ
 * ============================================================
 *
 * WHAT IS A PUBLISHER/PRODUCER?
 *   A publisher (also called a producer) is the part of the app
 *   that CREATES and SENDS a message to RabbitMQ.
 *
 *   Think of it like this:
 *     Publisher = The person who drops a letter in the mailbox.
 *     RabbitMQ  = The post office.
 *     Consumer  = The recipient who reads the letter.
 *
 * WHAT DOES THIS CLASS DO?
 *   When a user places an order, OrderService calls this class.
 *   This class takes an OrderCreatedEvent object and sends it
 *   to RabbitMQ so that the OrderEventConsumer can pick it up
 *   and process the order in the background.
 *
 * ANNOTATIONS EXPLAINED:
 *   @Service  → Tells Spring to manage this class as a bean.
 *               Spring will automatically inject it wherever needed.
 *   @Slf4j    → Lombok automatically creates a 'log' variable for us.
 *               We use log.info() to print status messages to the console.
 *   @RequiredArgsConstructor → Lombok creates a constructor that injects
 *               all 'final' fields automatically (RabbitTemplate here).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    /**
     * RabbitTemplate is our tool to SEND messages to RabbitMQ.
     * Spring automatically injects it because it's declared as 'final'.
     * We configured it with a JSON converter in RabbitMQConfig.java.
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * These values are read from application.properties.
     * @Value("${order.exchange}") injects the value of "order.exchange" property.
     * @Value("${order.routing-key}") injects "order.routing-key" property.
     */
    @Value("${order.exchange}")
    private String orderExchange;

    @Value("${order.routing-key}")
    private String orderRoutingKey;

    /**
     * Publishes an OrderCreatedEvent to RabbitMQ.
     *
     * HOW IT WORKS:
     *   1. We call rabbitTemplate.convertAndSend(...)
     *   2. RabbitTemplate serializes the OrderCreatedEvent to JSON.
     *   3. It sends the JSON message to the "order.exchange" exchange.
     *   4. The exchange routes it to "order.queue" (via our Binding).
     *   5. The message waits in the queue until OrderEventConsumer picks it up.
     *
     * PARAMETERS:
     *   exchange   → Which exchange to send the message to.
     *   routingKey → The label on the message (used by exchange for routing).
     *   event      → The actual message data (will be serialized to JSON).
     *
     * @param event the OrderCreatedEvent containing orderId, userId, createdAt
     */
    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Publishing order created event to RabbitMQ for orderId: {}", event.getOrderId());

        rabbitTemplate.convertAndSend(orderExchange, orderRoutingKey, event);

        log.info("Successfully published order created event for orderId: {} to exchange: '{}' with routing key: '{}'",
                event.getOrderId(), orderExchange, orderRoutingKey);
    }
}
