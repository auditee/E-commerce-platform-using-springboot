package com.ecommerce.exception;

/**
 * ============================================================
 * ResourceNotFoundException.java — Custom "not found" error
 * ============================================================
 *
 * WHAT IS A CUSTOM EXCEPTION?
 *   Java has built-in exceptions like NullPointerException.
 *   But those give ugly, confusing error messages.
 *   We create our OWN exception to give clean, helpful messages.
 *
 * WHEN IS THIS THROWN?
 *   When someone requests a product that doesn't exist.
 *   Example: GET /api/products/999
 *   If product 999 doesn't exist, we throw:
 *     throw new ResourceNotFoundException("Product not found with id: 999")
 *
 * WHY EXTEND RuntimeException?
 *   RuntimeException means Java doesn't FORCE us to catch it
 *   everywhere. Our GlobalExceptionHandler will catch it
 *   automatically and send a clean 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructor — takes a custom error message.
     * Example: new ResourceNotFoundException("Product not found with id: 5")
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
