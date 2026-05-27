package com.ecommerce.repository;

import com.ecommerce.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

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

    /**
     * ============================================================
     * findByIdForUpdate — Fetch a product WITH a database lock
     * ============================================================
     *
     * WHY DO WE NEED THIS METHOD?
     *   Imagine two users try to buy the last item at the same time:
     *
     *   Without locking:
     *     User A reads stock = 1, decides "enough stock", proceeds.
     *     User B reads stock = 1, decides "enough stock", proceeds.
     *     User A reduces stock → stock = 0
     *     User B reduces stock → stock = -1  ← WRONG! Negative stock!
     *
     *   With PESSIMISTIC_WRITE locking:
     *     User A reads stock = 1 → MySQL LOCKS that product row.
     *     User B tries to read the same row → BLOCKED. Must wait.
     *     User A reduces stock → stock = 0 → commits → LOCK RELEASED.
     *     User B reads stock = 0 → check fails → order rejected. ✅
     *
     * HOW DOES @Lock(LockModeType.PESSIMISTIC_WRITE) WORK?
     *   It tells MySQL to run: SELECT ... FOR UPDATE
     *   "FOR UPDATE" is a SQL command that places an exclusive lock
     *   on the selected row for the duration of the current transaction.
     *   No other transaction can read or modify that row until the
     *   first transaction commits or rolls back.
     *
     * WHY USE @Query?
     *   Spring Data JPA needs an explicit JPQL query to apply a lock.
     *   Without @Query, @Lock won't attach properly to the SQL statement.
     *
     * WHY @Param("id")?
     *   The :id placeholder in the query is filled with the value of
     *   the 'id' parameter passed to this method, using @Param("id").
     *
     * IMPORTANT: This method MUST be called inside a @Transactional method.
     *   Pessimistic locks only exist within an active database transaction.
     *   When the transaction ends, the lock is automatically released.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
