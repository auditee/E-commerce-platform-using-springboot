package com.ecommerce.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * ============================================================
 * ProductRequest.java — The "incoming data" DTO
 * ============================================================
 *
 * WHAT IS A DTO?
 *   DTO = Data Transfer Object. It's a simple container used to
 *   carry data FROM the frontend (or Postman) TO our backend.
 *
 * WHY NOT USE THE PRODUCT ENTITY DIRECTLY?
 *   Because the entity has extra fields like id, createdAt, and
 *   updatedAt that the user should NOT send. The DTO only contains
 *   the fields the user is allowed to provide.
 *
 * WHAT IS VALIDATION?
 *   Validation means checking if the data the user sent makes sense.
 *   For example:
 *     - "name" should not be empty.
 *     - "price" should not be negative.
 *   If the user sends bad data, Spring automatically rejects it
 *   and sends a 400 Bad Request error — before our code even runs.
 *
 * VALIDATION ANNOTATIONS EXPLAINED:
 *   @NotBlank → Field must not be null, empty, or just spaces.
 *   @NotNull  → Field must not be null (but can be 0 or empty string).
 *   @Positive → Number must be greater than 0.
 *   @PositiveOrZero → Number must be 0 or more.
 *
 * LOMBOK:
 *   @Data → Shortcut that combines @Getter, @Setter, @ToString,
 *           @EqualsAndHashCode, and @RequiredArgsConstructor.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "Product description is required")
    private String description;

    @NotNull(message = "Product price is required")
    @Positive(message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @PositiveOrZero(message = "Stock quantity must be 0 or more")
    private Integer stockQuantity;

    @NotBlank(message = "Product category is required")
    private String category;

    // imageUrl is optional — no validation needed
    private String imageUrl;
}
