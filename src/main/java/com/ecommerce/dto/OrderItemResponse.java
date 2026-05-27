package com.ecommerce.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * ============================================================
 * OrderItemResponse.java — Data transfer object representing a purchased item
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   Format and return order item information to the user.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long orderItemId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}
