package com.ecommerce.entity;

/**
 * ============================================================
 * OrderStatus.java — The Order Status Enum
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It defines the set of allowed states an Order can be in:
 *     - PENDING: Order is created but payment/confirmation is not complete.
 *     - CONFIRMED: Order has been successfully validated and placed (default for now).
 *     - CANCELLED: Order has been cancelled.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
}
