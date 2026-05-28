package com.ecommerce.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ============================================================
 * OrderResponse.java — Data transfer object representing a completed order
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   Format and return full order information (including user details, items,
 *   total amount, date, and status) to the frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long orderId;
    private Long userId;
    private String userName;
    private List<OrderItemResponse> items;
    private BigDecimal totalAmount;
    private String status;
    private String paymentStatus;
    private String paymentMode;
    private String paymentReference;
    private LocalDateTime paidAt;
    private String message;
    private String shippingAddress;
    private String phoneNumber;
    private LocalDateTime orderDate;
    private Integer totalItems;
}
