/**
 * DTO (Data Transfer Object) Package
 * ====================================
 * This package contains DTO classes.
 * DTOs are used to safely carry data between the frontend and backend.
 *
 * Why not just use entities directly?
 *   - Entities contain EVERYTHING from the database, including
 *     sensitive data like passwords.
 *   - DTOs let us control exactly what data goes in and out.
 *
 * Example:
 *   - LoginRequest DTO  → carries email + password FROM the frontend.
 *   - LoginResponse DTO → carries a JWT token BACK to the frontend.
 *     (We never send the password back!)
 */
package com.ecommerce.dto;
