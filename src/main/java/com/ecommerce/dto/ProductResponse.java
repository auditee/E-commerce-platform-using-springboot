package com.ecommerce.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 * ProductResponse.java — The "outgoing data" DTO
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   When someone requests a product (GET /api/products/1),
 *   we don't send the raw database entity back. Instead, we
 *   convert the entity into this clean ProductResponse object.
 *
 * WHY USE A SEPARATE RESPONSE DTO?
 *   1. Control: We choose exactly which fields to send back.
 *   2. Safety: If the entity had a password field, we could
 *      exclude it here. (Products don't, but Users will.)
 *   3. Flexibility: We can add computed fields (like discountedPrice)
 *      without changing the database entity.
 *
 * This DTO includes ALL product fields, including id, createdAt,
 * and updatedAt — because the frontend needs to see those.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stockQuantity;
    private String category;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
