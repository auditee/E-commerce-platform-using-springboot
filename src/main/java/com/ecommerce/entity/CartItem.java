package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 * CartItem.java — The CartItem Entity
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   This represents the "cart_items" table in our MySQL database.
 *   It serves as a join table linking a user's Cart with the Products they want,
 *   along with the desired quantity, price snapshot, and subtotal.
 *
 * RELATIONSHIPS:
 *   - ManyToOne with Cart: Many cart items can belong to a single cart.
 *   - ManyToOne with Product: Many cart items can point to a single product.
 */
@Entity
@Table(name = "cart_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The cart this item belongs to.
     * We use FetchType.LAZY because we don't always need to load the full cart
     * when looking at a cart item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    /**
     * The product added to the cart.
     * We use FetchType.EAGER because we always need the product details
     * (name, price, stock, image) whenever we look at a cart item.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * The quantity of this product in the cart.
     * Must be positive.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Snapshot of the product price when it was added to the cart.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Calculated subtotal (price * quantity).
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Auto-calculate subtotal if price and quantity are set
        if (subtotal == null && price != null && quantity != null) {
            subtotal = price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Recalculate subtotal on updates
        if (price != null && quantity != null) {
            subtotal = price.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
