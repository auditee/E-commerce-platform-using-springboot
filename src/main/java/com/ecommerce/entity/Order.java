package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 * Order.java — The Order Entity
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   This represents the "orders" table in our MySQL database.
 *   Each Order belongs to a specific User and contains one or more OrderItems.
 *
 * RELATIONSHIPS:
 *   - ManyToOne with User: One user can have many orders.
 *   - OneToMany with OrderItem: One order can contain multiple order items.
 *     We use cascade = CascadeType.ALL and orphanRemoval = true so that items 
 *     are saved and deleted automatically when we update the order.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who placed this order.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The list of items belonging to this order.
     * We initialize it to an empty list to avoid NullPointerExceptions.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * The total amount of the order (sum of all item subtotals).
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * The status of the order (PENDING, CONFIRMED, CANCELLED).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /**
     * The shipping address for this order.
     */
    @Column(nullable = false, length = 500)
    private String shippingAddress;

    /**
     * The contact phone number for delivery.
     */
    @Column(nullable = false, length = 20)
    private String phoneNumber;

    /**
     * The date and time when the order was placed.
     */
    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
