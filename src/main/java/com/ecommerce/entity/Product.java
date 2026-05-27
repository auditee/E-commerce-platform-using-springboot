package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ============================================================
 * Product.java — The Product Entity
 * ============================================================
 *
 * WHAT IS AN ENTITY?
 *   An entity is a Java class that represents a table in the
 *   database. Each object of this class = one row in the table.
 *   Each field = one column in the table.
 *
 * WHAT DOES THIS FILE DO?
 *   This file tells Hibernate (the JPA engine) to create a
 *   table called "products" in MySQL with columns like name,
 *   description, price, etc.
 *
 * ANNOTATIONS EXPLAINED:
 *   @Entity         → Marks this class as a database table.
 *   @Table          → Specifies the table name in the database.
 *   @Id             → Marks the primary key field.
 *   @GeneratedValue → The database auto-generates the ID (1, 2, 3...).
 *   @Column         → Configures column properties (nullable, length).
 *   @PrePersist     → A method that runs BEFORE a new row is saved.
 *   @PreUpdate      → A method that runs BEFORE an existing row is updated.
 *
 * LOMBOK ANNOTATIONS EXPLAINED:
 *   @Getter         → Automatically creates getter methods (getName(), getPrice()...).
 *   @Setter         → Automatically creates setter methods (setName(), setPrice()...).
 *   @NoArgsConstructor  → Creates an empty constructor: new Product().
 *   @AllArgsConstructor → Creates a constructor with ALL fields as parameters.
 *   @Builder        → Lets us create objects like: Product.builder().name("iPhone").build()
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    /**
     * Primary Key — unique identifier for each product.
     * GenerationType.IDENTITY means MySQL auto-increments this (1, 2, 3...).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Product name — e.g. "iPhone 15".
     * nullable = false means this column CANNOT be empty in the database.
     * length = 200 means maximum 200 characters.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Product description — e.g. "Apple iPhone 15 with 128GB storage".
     * columnDefinition = "TEXT" allows longer text (more than 255 characters).
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Product price — e.g. 79999.00.
     * We use BigDecimal instead of double for money because
     * double can have tiny rounding errors (like 0.1 + 0.2 = 0.30000000004).
     * BigDecimal is precise.
     * precision = 10 means up to 10 total digits.
     * scale = 2 means 2 digits after the decimal point.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /**
     * How many units of this product are in stock.
     * e.g. 10 iPhones available.
     */
    @Column(nullable = false)
    private Integer stockQuantity;

    /**
     * Product category — e.g. "Mobile", "Laptop", "Clothing".
     */
    @Column(nullable = false, length = 100)
    private String category;

    /**
     * URL to the product image.
     * This is optional — a product might not have an image yet.
     * length = 500 to allow long URLs.
     */
    @Column(length = 500)
    private String imageUrl;

    /**
     * When this product was first added to the database.
     * updatable = false means once set, it never changes.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this product was last modified.
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * @PrePersist runs automatically BEFORE a new product is saved
     * to the database for the first time.
     * It sets both createdAt and updatedAt to the current date and time.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * @PreUpdate runs automatically BEFORE an existing product is
     * updated in the database.
     * It updates the updatedAt timestamp to the current date and time.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
