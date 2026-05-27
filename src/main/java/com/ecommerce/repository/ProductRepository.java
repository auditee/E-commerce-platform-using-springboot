package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * ============================================================
 * ProductRepository.java — The database access layer
 * ============================================================
 *
 * WHAT IS A REPOSITORY?
 *   A repository is the layer that directly talks to the database.
 *   It can save, find, update, and delete data.
 *
 * WHY IS THIS AN INTERFACE (NOT A CLASS)?
 *   This is the magic of Spring Data JPA!
 *   We only write the interface (the "contract"), and Spring
 *   automatically provides the full implementation at runtime.
 *
 * WHAT IS JpaRepository<Product, Long>?
 *   - Product → The entity this repository manages.
 *   - Long    → The data type of the primary key (id).
 *
 * METHODS WE GET FOR FREE (without writing any code):
 *   .save(product)       → INSERT or UPDATE a product
 *   .findById(id)        → SELECT a product by its ID
 *   .findAll()           → SELECT all products
 *   .deleteById(id)      → DELETE a product by its ID
 *   .count()             → COUNT total products
 *   .existsById(id)      → Check if a product exists
 *
 * CUSTOM METHODS BELOW:
 *   Spring Data JPA can also create queries from method names!
 *   "findByCategoryIgnoreCase" becomes:
 *     SELECT * FROM products WHERE LOWER(category) = LOWER(?)
 *   "findByNameContainingIgnoreCase" becomes:
 *     SELECT * FROM products WHERE LOWER(name) LIKE LOWER('%keyword%')
 *
 *   You don't write a single line of SQL!
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find all products in a specific category.
     * "IgnoreCase" means "Mobile" and "mobile" are treated the same.
     *
     * Example: findByCategoryIgnoreCase("Mobile")
     * SQL:     SELECT * FROM products WHERE LOWER(category) = LOWER('Mobile')
     */
    List<Product> findByCategoryIgnoreCase(String category);

    /**
     * Search products whose name contains a keyword.
     * "Containing" means partial match (LIKE %keyword%).
     * "IgnoreCase" means case doesn't matter.
     *
     * Example: findByNameContainingIgnoreCase("phone")
     * SQL:     SELECT * FROM products WHERE LOWER(name) LIKE LOWER('%phone%')
     */
    List<Product> findByNameContainingIgnoreCase(String name);
}
