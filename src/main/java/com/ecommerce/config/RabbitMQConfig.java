package com.ecommerce.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ============================================================
 * RabbitMQConfig.java — Configures our RabbitMQ setup
 * ============================================================
 *
 * WHAT IS RABBITMQ?
 *   RabbitMQ is a message broker. A message broker is a middleman
 *   that receives messages from one part of your app (the producer)
 *   and delivers them to another part (the consumer).
 *
 *   Real-world analogy:
 *     Post Office = RabbitMQ
 *     You sending a parcel = OrderService publishing an event
 *     The delivery person = RabbitMQ routing the message
 *     The recipient = OrderEventConsumer receiving and processing it
 *
 *   Why is this useful?
 *     Without RabbitMQ (synchronous flow):
 *       User waits for stock check + order save + cart clear before getting a response.
 *       If stock check takes 500ms, user waits 500ms.
 *       If anything fails, user gets an error immediately.
 *
 *     With RabbitMQ (asynchronous flow):
 *       User gets an instant response: "Order placed! Status: PENDING"
 *       Heavy processing happens in the background.
 *       User can check back later for CONFIRMED or FAILED status.
 *       This is how real e-commerce platforms like Amazon work!
 *
 * KEY RABBITMQ CONCEPTS:
 *
 *   QUEUE:
 *     A queue is like a "waiting line" or "mailbox" for messages.
 *     Messages sit in the queue until a consumer picks them up.
 *     Our queue is named "order.queue".
 *
 *   EXCHANGE:
 *     An exchange is the "sorting office". Messages arrive at the
 *     exchange, and the exchange decides which queue to send them to
 *     based on the routing key.
 *     We use a DirectExchange, which is the simplest type:
 *     it routes messages to a queue whose binding key exactly matches
 *     the routing key.
 *
 *   ROUTING KEY:
 *     A routing key is a label on a message. It's like the address
 *     written on an envelope. The exchange reads this label and
 *     routes the message to the matching queue.
 *     Our routing key is "order.created".
 *
 *   BINDING:
 *     A binding is the rule that links an exchange to a queue via
 *     a routing key. It says:
 *     "Messages arriving at 'order.exchange' with routing key
 *      'order.created' → go to 'order.queue'."
 *
 * HOW IT ALL FITS TOGETHER:
 *
 *   OrderService publishes event
 *          ↓
 *   RabbitTemplate sends to → order.exchange (with routing key: order.created)
 *          ↓
 *   order.exchange routes to → order.queue  (via Binding)
 *          ↓
 *   order.queue holds message until consumer is ready
 *          ↓
 *   OrderEventConsumer picks up message and processes order
 *
 * WHY JSON CONVERTER?
 *   By default RabbitMQ sends messages as raw bytes (binary format).
 *   With Jackson2JsonMessageConverter, our OrderCreatedEvent Java
 *   object is automatically converted to/from JSON text before
 *   being sent over RabbitMQ. This makes messages human-readable
 *   in the RabbitMQ dashboard (http://localhost:15672).
 */
@Configuration
public class RabbitMQConfig {

    /**
     * These values come from application.properties.
     * @Value("${order.queue}") reads the "order.queue" property
     * and injects it as a String into this field.
     */
    @Value("${order.queue}")
    private String orderQueue;

    @Value("${order.exchange}")
    private String orderExchange;

    @Value("${order.routing-key}")
    private String orderRoutingKey;

    /**
     * Creates the RabbitMQ Queue.
     *
     * Queue name: "order.queue" (from application.properties)
     * durable = true → The queue survives a RabbitMQ server restart.
     *   If false, the queue disappears when RabbitMQ restarts,
     *   and all unprocessed messages are lost!
     *   Setting durable = true protects against this.
     */
    @Bean
    public Queue orderQueue() {
        return new Queue(orderQueue, true);
    }

    /**
     * Creates a DirectExchange.
     *
     * Exchange name: "order.exchange" (from application.properties)
     * DirectExchange routes messages to queues where the binding key
     * exactly matches the routing key of the published message.
     *
     * Analogy: A sorting machine that reads the exact address on a
     * parcel and places it in the exactly matching delivery bin.
     */
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(orderExchange);
    }

    /**
     * Creates the Binding between exchange and queue.
     *
     * This tells RabbitMQ:
     *   "When a message arrives at 'order.exchange' with routing key
     *    'order.created', put it in the 'order.queue' queue."
     *
     * Without this binding, the exchange doesn't know which queue
     * to deliver messages to — they would be dropped!
     */
    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder
                .bind(orderQueue)
                .to(orderExchange)
                .with(orderRoutingKey);
    }

    /**
     * Creates a JSON message converter.
     *
     * This makes RabbitMQ serialize/deserialize messages as JSON
     * instead of raw Java bytes (default serialization).
     *
     * WITHOUT converter:
     *   Message in queue: [binary garbage — unreadable]
     *
     * WITH converter:
     *   Message in queue: {"orderId":42,"userId":5,"createdAt":"2026-05-27T19:00:00"}
     *   → Readable in the RabbitMQ dashboard!
     *   → Also works correctly between different Java versions.
     */
    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configures the RabbitTemplate to use our JSON converter.
     *
     * RabbitTemplate is the main tool we use to SEND messages.
     * Think of it like a "post service counter" — we hand it a
     * message and it handles the delivery to RabbitMQ.
     *
     * We must tell it to use our JSON converter, otherwise it
     * will use raw binary serialization.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jacksonMessageConverter());
        return rabbitTemplate;
    }
}
