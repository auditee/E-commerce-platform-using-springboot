package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ============================================================
 * LoginRequest.java — Data the user sends to log in
 * ============================================================
 *
 * When an existing user wants to log in, they send their
 * email and password to POST /api/auth/login.
 *
 * If the credentials are valid, the server returns a JWT token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
