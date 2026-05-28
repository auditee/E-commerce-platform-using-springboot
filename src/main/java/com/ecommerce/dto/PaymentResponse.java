package com.ecommerce.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 * PaymentResponse.java — Simulated Payment Response DTO
 * ============================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long orderId;
    private String paymentStatus;
    private String paymentReference;
    private String paymentMode;
    private BigDecimal amountPaid;
    private LocalDateTime paidAt;
    private String message;
}
