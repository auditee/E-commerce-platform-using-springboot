package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 * OrderItem.java — The OrderItem Entity
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   This represents the "order_items" table in our MySQL database.
 *   It stores the details of a single product inside a completed Order.
 *
 * IMPORTANT DESIGN DECISION:
 *   Why do we duplicate productName, productImageUrl, and price here?
 *   If a shop admin updates the product name or price later (e.g. price rises from $10 to $15),
 *   the historical order data should NOT change. 
 *   Storing these details in the OrderItem ensures the order history remains correct forever.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The order this item belongs to.
     * We use LAZY fetch since we don't need to load the full Order when looking at an item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * The original product.
     * We keep a link to the product table. We use SET_NULL or standard association.
     * For learning purposes, a simple ManyToOne with Product is sufficient.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Snapshot of the product name at checkout time.
     */
    @Column(nullable = false, length = 200)
    private String productName;

    /**
     * Snapshot of the product image URL at checkout time.
     */
    @Column(length = 500)
    private String productImageUrl;

    /**
     * Snapshot of the product price at checkout time.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * Quantity of this product ordered.
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Calculated subtotal (price * quantity) stored in the database.
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
        
        // Ensure subtotal is calculated if not set
        if (subtotal == null && price != null && quantity != null) {
            subtotal = price.multiply(BigDecimal.valueOf(quantity));
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        
        // Recalculate subtotal on update if needed
        if (price != null && quantity != null) {
            subtotal = price.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
