package com.ecommerce.controller;

import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 * AuthController.java — Registration and Login API endpoints
 * ============================================================
 *
 * This controller handles two things:
 *   1. POST /api/auth/register → Create a new user account.
 *   2. POST /api/auth/login    → Log in and get a JWT token.
 *
 * Both endpoints are PUBLIC — anyone can access them without
 * a token (because you can't have a token before you log in!).
 *
 * RESPONSE:
 *   Both endpoints return an AuthResponse containing:
 *   {
 *     "token": "eyJhbGci...",
 *     "tokenType": "Bearer",
 *     "userId": 1,
 *     "name": "Auditee",
 *     "email": "auditee@example.com",
 *     "role": "USER"
 *   }
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // =========================================================
    // POST /api/auth/register — Create a new account
    // =========================================================
    /**
     * Registers a new user.
     *
     * Request body example:
     * {
     *   "name": "Auditee",
     *   "email": "auditee@example.com",
     *   "password": "123456",
     *   "role": "USER"          ← optional, defaults to USER
     * }
     *
     * Returns 201 CREATED with JWT token on success.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);  // 201
    }

    // =========================================================
    // POST /api/auth/login — Log in to an existing account
    // =========================================================
    /**
     * Authenticates a user and returns a JWT token.
     *
     * Request body example:
     * {
     *   "email": "auditee@example.com",
     *   "password": "123456"
     * }
     *
     * Returns 200 OK with JWT token on success.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);  // 200
    }
}
