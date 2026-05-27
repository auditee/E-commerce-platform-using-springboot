package com.ecommerce.repository;

import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * ============================================================
 * CartItemRepository.java — The CartItem Database Access Interface
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It provides standard database operations for individual items in a user's cart.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Finds a specific cart item linking a cart and a product.
     * This is useful to check if a product is already added in the user's cart.
     */
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);

    /**
     * Finds a cart item by its ID and the parent Cart.
     * This acts as an ownership check, ensuring users only update/delete items from their own cart.
     */
    Optional<CartItem> findByIdAndCart(Long id, Cart cart);
}
