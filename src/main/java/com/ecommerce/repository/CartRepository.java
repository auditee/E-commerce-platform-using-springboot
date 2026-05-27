package com.ecommerce.repository;

import com.ecommerce.entity.Cart;
import com.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * ============================================================
 * CartRepository.java — The Cart Database Access Interface
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It provides standard database operations (save, delete, findById) for the Cart entity.
 *   Spring Data JPA automatically implements this interface at runtime.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Finds a cart belonging to a specific user object.
     */
    Optional<Cart> findByUser(User user);

    /**
     * Finds a cart by its associated user's ID.
     */
    Optional<Cart> findByUserId(Long userId);
}
