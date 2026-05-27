package com.ecommerce.entity;

/**
 * ============================================================
 * Role.java — The Role Enum
 * ============================================================
 *
 * WHAT IS AN ENUM?
 *   An enum is a special Java type that has a fixed set of values.
 *   Think of it like a dropdown menu — you can ONLY pick from
 *   the listed options, nothing else.
 *
 * WHAT ROLES DO WE HAVE?
 *   - USER  → Normal customer. Can browse and buy products.
 *   - ADMIN → Store manager. Can add, update, and delete products.
 *
 * HOW IS IT USED?
 *   When a user registers, they are assigned one of these roles.
 *   Spring Security then checks the role before allowing access
 *   to protected APIs.
 */
public enum Role {
    USER,
    ADMIN
}
