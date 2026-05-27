package com.ecommerce.repository;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================
 * OrderRepository.java — The Order Database Access Interface
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It provides database operations for the Order entity.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds all orders placed by a specific user, sorted from newest to oldest.
     * This is used to display the user's order history.
     */
    List<Order> findByUserOrderByOrderDateDesc(User user);

    /**
     * Finds a specific order by its ID and user owner.
     * This acts as an authorization boundary: a user can only query their own orders.
     */
    Optional<Order> findByIdAndUser(Long id, User user);
}
