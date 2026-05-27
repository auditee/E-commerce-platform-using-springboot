/**
 * Exception Package
 * =================
 * This package contains custom exception (error) classes.
 *
 * Instead of our app crashing with ugly error messages,
 * we create custom exceptions that return clean JSON responses.
 *
 * Example:
 *   Instead of a 500 Internal Server Error, we send:
 *   {
 *     "message": "Product not found with id: 99",
 *     "status": 404
 *   }
 */
package com.ecommerce.exception;
