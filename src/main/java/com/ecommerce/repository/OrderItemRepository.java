package com.ecommerce.repository;

import com.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ============================================================
 * OrderItemRepository.java — The OrderItem Database Access Interface
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It provides standard database operations for order items.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
