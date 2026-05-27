package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ============================================================
 * User.java — The User Entity
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   This class represents the "users" table in our MySQL database.
 *   Each User object = one row in the table.
 *
 * IMPORTANT FIELDS:
 *   - email: Must be unique. Two users cannot register with
 *            the same email address.
 *   - password: Stored as an ENCRYPTED hash (not plain text!).
 *               Even if someone steals the database, they
 *               cannot read the real passwords.
 *   - role: Either USER or ADMIN. Stored as a string in the
 *           database (e.g., "USER" or "ADMIN") because we use
 *           @Enumerated(EnumType.STRING).
 *
 * IMPLEMENTS UserDetails?
 *   No — we keep our entity clean and simple. Instead, we create
 *   a separate CustomUserDetailsService that converts this User
 *   into a Spring Security UserDetails object when needed.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Primary Key — auto-generated unique ID for each user.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User's display name — e.g., "Auditee".
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * User's email — used for login.
     * unique = true means no two users can have the same email.
     */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /**
     * User's password — stored as an encrypted BCrypt hash.
     * Example: "$2a$10$dXJ3SW6G7P50lGmMQoeaiu..."
     * The real password is NEVER stored in the database.
     */
    @Column(nullable = false)
    private String password;

    /**
     * User's role — either USER or ADMIN.
     * @Enumerated(EnumType.STRING) stores the role name as text
     * in the database (e.g., "USER") instead of a number (0, 1).
     * This is easier to read and debug.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * When this user account was created.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this user account was last updated.
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Runs automatically BEFORE a new user is saved for the first time.
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Runs automatically BEFORE an existing user is updated.
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
