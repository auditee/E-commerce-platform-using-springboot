package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * ============================================================
 * PaymentRequest.java — Simulated Payment Request DTO
 * ============================================================
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Payment mode is required")
    private String paymentMode;

    @NotBlank(message = "Payment result is required (SUCCESS/FAILED)")
    private String paymentResult;
}
