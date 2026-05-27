package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * Cart.java — The Cart Entity
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   This represents the "carts" table in our MySQL database.
 *   Each logged-in user will have exactly one Cart to hold their selected products.
 *
 * RELATIONSHIPS:
 *   - OneToOne with User: Each cart belongs to exactly one user.
 *   - OneToMany with CartItem: A cart can contain multiple items.
 *     We use cascade = CascadeType.ALL so that when we save or delete a Cart, 
 *     its items are automatically saved or deleted too.
 *     orphanRemoval = true ensures that if we remove an item from the list, 
 *     it is automatically deleted from the database table.
 */
@Entity
@Table(name = "carts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who owns this cart.
     * We map this as OneToOne. A user has one cart.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The list of items inside this cart.
     * We initialize it to an empty list to avoid NullPointerExceptions.
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    /**
     * Helper method to add a cart item and set the bidirectional relationship.
     */
    public void addItem(CartItem item) {
        if (this.cartItems == null) {
            this.cartItems = new ArrayList<>();
        }
        this.cartItems.add(item);
        item.setCart(this);
    }

    /**
     * Helper method to remove a cart item and break the bidirectional relationship.
     */
    public void removeItem(CartItem item) {
        if (this.cartItems != null) {
            this.cartItems.remove(item);
            item.setCart(null);
        }
    }

    /**
     * The total amount of the cart (sum of all cart item subtotals).
     */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalAmount == null) {
            totalAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
