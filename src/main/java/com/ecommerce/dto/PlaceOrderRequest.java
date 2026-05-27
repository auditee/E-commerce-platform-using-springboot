package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/**
 * ============================================================
 * PlaceOrderRequest.java — Data transfer object for placing an order
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It carries and validates checkout details from the user.
 *   Example JSON request:
 *   {
 *     "shippingAddress": "Siliguri, West Bengal, India",
 *     "phoneNumber": "9876543210"
 *   }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceOrderRequest {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;
}
