package com.ecommerce.service;

import com.ecommerce.dto.OrderCreatedEvent;
import com.ecommerce.dto.PaymentRequest;
import com.ecommerce.dto.PaymentResponse;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.PaymentStatus;
import com.ecommerce.entity.User;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.messaging.OrderEventPublisher;
import com.ecommerce.repository.OrderRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ============================================================
 * PaymentService.java — Simulated Payment Gateway Logic
 * ============================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderEventPublisher orderEventPublisher;

    /**
     * Processes a simulated payment for a pending order.
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request, String userEmail) {
        log.info("Simulated payment initiated for orderId: {} by user: {}", request.getOrderId(), userEmail);

        // 1. Fetch user and order
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));

        // 2. Validate ownership
        if (!order.getUser().getId().equals(user.getId())) {
            log.warn("Access denied: User {} does not own order {}", userEmail, order.getId());
            throw new RuntimeException("You do not have permission to pay for this order");
        }

        // 3. Validate status (must be PENDING_PAYMENT)
        if (!order.getStatus().equals(OrderStatus.PENDING_PAYMENT)) {
            log.warn("Invalid status: Order {} is already in status '{}'", order.getId(), order.getStatus());
            throw new RuntimeException("Payment can only be processed for orders in PENDING_PAYMENT status");
        }

        String reference = "SIM-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // 4. Handle success flow
        if ("SUCCESS".equalsIgnoreCase(request.getPaymentResult())) {
            log.info("Simulated payment SUCCESS for orderId: {}. Reference: {}", order.getId(), reference);

            // Update payment fields
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaymentMode(request.getPaymentMode().toUpperCase());
            order.setPaymentReference(reference);
            order.setPaidAt(LocalDateTime.now());

            // Set order status to PENDING (paid, awaiting RabbitMQ stock confirm)
            order.setStatus(OrderStatus.PENDING);
            Order savedOrder = orderRepository.save(order);

            // Publish OrderCreatedEvent to RabbitMQ to check stock & clear cart
            log.info("Publishing OrderCreatedEvent for paid orderId: {}", savedOrder.getId());
            OrderCreatedEvent event = OrderCreatedEvent.builder()
                    .orderId(savedOrder.getId())
                    .userId(user.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            orderEventPublisher.publishOrderCreatedEvent(event);

            return PaymentResponse.builder()
                    .orderId(savedOrder.getId())
                    .paymentStatus(PaymentStatus.PAID.name())
                    .paymentMode(savedOrder.getPaymentMode())
                    .paymentReference(savedOrder.getPaymentReference())
                    .amountPaid(savedOrder.getTotalAmount())
                    .paidAt(savedOrder.getPaidAt())
                    .message("Payment successful. Your order is being processed.")
                    .build();

        } else {
            // 5. Handle failed flow
            log.warn("Simulated payment FAILED for orderId: {}. Reference: {}", order.getId(), reference);

            // Update payment fields
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setPaymentMode(request.getPaymentMode().toUpperCase());
            order.setPaymentReference(reference);

            // Set order status to FAILED
            order.setStatus(OrderStatus.FAILED);
            Order savedOrder = orderRepository.save(order);

            return PaymentResponse.builder()
                    .orderId(savedOrder.getId())
                    .paymentStatus(PaymentStatus.FAILED.name())
                    .paymentMode(savedOrder.getPaymentMode())
                    .paymentReference(savedOrder.getPaymentReference())
                    .amountPaid(savedOrder.getTotalAmount())
                    .paidAt(null)
                    .message("Payment failed. The order has been marked as FAILED.")
                    .build();
        }
    }

    /**
     * Retrieves the payment status of an order.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentDetails(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Validate ownership
        if (!order.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You do not have permission to view payment details for this order");
        }

        return PaymentResponse.builder()
                .orderId(order.getId())
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .paymentMode(order.getPaymentMode())
                .paymentReference(order.getPaymentReference())
                .amountPaid(order.getTotalAmount())
                .paidAt(order.getPaidAt())
                .message("Order payment status is: " + order.getPaymentStatus())
                .build();
    }
}
