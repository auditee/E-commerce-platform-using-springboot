package com.ecommerce.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * ============================================================
 * CartResponse.java — Data transfer object representing the full cart state
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   Returns the formatted cart contents, total price, and summary details.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {
    private Long cartId;
    private Long userId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;
    private Integer totalItems;
}
