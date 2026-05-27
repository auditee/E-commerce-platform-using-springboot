package com.ecommerce.dto;

import lombok.*;

/**
 * ============================================================
 * AuthResponse.java — Data sent back after login/register
 * ============================================================
 *
 * After a successful login or registration, the server sends
 * this response containing:
 *   - token: The JWT token (the "digital key").
 *   - tokenType: Always "Bearer" (an HTTP standard).
 *   - userId, name, email, role: Basic user info.
 *
 * The frontend saves the token and includes it in the
 * "Authorization" header of future requests:
 *   Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;
    private String name;
    private String email;
    private String role;
}
