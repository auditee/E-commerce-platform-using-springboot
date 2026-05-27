package com.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * ============================================================
 * AddToCartRequest.java — Data transfer object for adding products
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It holds and validates the input when a user wants to add a product to their cart.
 *   Example JSON request:
 *   {
 *     "productId": 1,
 *     "quantity": 2
 *   }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
