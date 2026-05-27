package com.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * ============================================================
 * UpdateCartItemRequest.java — Data transfer object for updating quantities
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It carries and validates the input when a user wants to update
 *   the quantity of a product in their cart.
 *   Example JSON request:
 *   {
 *     "quantity": 3
 *   }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCartItemRequest {

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
