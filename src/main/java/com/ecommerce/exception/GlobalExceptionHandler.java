package com.ecommerce.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================
 * GlobalExceptionHandler.java — Catches ALL errors in one place
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   Without this file, when an error occurs, Spring sends an
 *   ugly default error page. This file intercepts those errors
 *   and sends clean, readable JSON responses instead.
 *
 * ERRORS WE HANDLE:
 *   1. ResourceNotFoundException     → 404 Not Found
 *   2. MethodArgumentNotValidException → 400 Validation errors
 *   3. BadCredentialsException       → 401 Wrong email or password
 *   4. AccessDeniedException         → 403 Not allowed (wrong role)
 *   5. RuntimeException              → 400 Business logic errors
 *   6. Exception                     → 500 Unexpected server errors
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles: ResourceNotFoundException
     * When: Someone requests a product/user that doesn't exist.
     * Returns: HTTP 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("message", ex.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles: MethodArgumentNotValidException
     * When: The user sends invalid data (e.g. blank name, bad email format).
     * Returns: HTTP 400 Bad Request with field-level error details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage())
        );

        Map<String, Object> error = new HashMap<>();
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("message", "Validation failed");
        error.put("errors", fieldErrors);
        error.put("timestamp", LocalDateTime.now().toString());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles: BadCredentialsException
     * When: User tries to log in with wrong email or password.
     * Returns: HTTP 401 Unauthorized
     *
     * Example response:
     * {
     *   "status": 401,
     *   "message": "Invalid email or password",
     *   "timestamp": "2026-05-26T10:30:00"
     * }
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", HttpStatus.UNAUTHORIZED.value());
        error.put("message", "Invalid email or password");
        error.put("timestamp", LocalDateTime.now().toString());

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles: AccessDeniedException
     * When: A USER tries to access an ADMIN-only endpoint.
     * Returns: HTTP 403 Forbidden
     *
     * Example response:
     * {
     *   "status": 403,
     *   "message": "You do not have permission to access this resource",
     *   "timestamp": "2026-05-26T10:30:00"
     * }
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", HttpStatus.FORBIDDEN.value());
        error.put("message", "You do not have permission to access this resource");
        error.put("timestamp", LocalDateTime.now().toString());

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles: RuntimeException
     * When: Business logic errors (e.g. "Email already exists").
     * Returns: HTTP 400 Bad Request
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("message", ex.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles: Any other unexpected Exception
     * When: Something unexpected goes wrong (database down, etc.)
     * Returns: HTTP 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(
            Exception ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        error.put("message", "Something went wrong: " + ex.getMessage());
        error.put("timestamp", LocalDateTime.now().toString());

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
