package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ============================================================
 * RegisterRequest.java — Data the user sends to sign up
 * ============================================================
 *
 * When a new user wants to create an account, they send this
 * data to POST /api/auth/register.
 *
 * VALIDATION RULES:
 *   - name: Required, cannot be blank.
 *   - email: Required, must look like a real email.
 *   - password: Required, at least 6 characters.
 *   - role: Optional. If not provided, defaults to USER.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    /**
     * Role is optional. If user doesn't send a role,
     * the AuthService will default it to USER.
     * Valid values: "USER" or "ADMIN"
     */
    private String role;
}
