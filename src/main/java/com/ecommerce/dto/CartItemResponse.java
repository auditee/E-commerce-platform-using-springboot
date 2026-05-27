package com.ecommerce.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * ============================================================
 * CartItemResponse.java — Data transfer object for returning cart item details
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It is used to format cart item data returned to the frontend.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subtotal;
}
