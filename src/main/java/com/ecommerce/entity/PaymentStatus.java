package com.ecommerce.entity;

/**
 * ============================================================
 * PaymentStatus.java — The Payment Status Enum
 * ============================================================
 *
 * Mapped to the paymentStatus column in the orders table.
 * It tracks the exact state of payment for an order.
 */
public enum PaymentStatus {
    /**
     * Payment has not been made yet. Initial state.
     */
    UNPAID,

    /**
     * Payment was processed successfully.
     */
    PAID,

    /**
     * Payment failed or was rejected.
     */
    FAILED
}
