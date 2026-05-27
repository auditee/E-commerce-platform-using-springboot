package com.ecommerce.repository;

import com.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * ============================================================
 * UserRepository.java — Database access for Users
 * ============================================================
 *
 * Just like ProductRepository, this interface lets us perform
 * database operations on the "users" table without writing SQL.
 *
 * CUSTOM METHODS:
 *   findByEmail → Used during login to find the user by email.
 *   existsByEmail → Used during registration to check if an
 *                   email is already taken.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their email address.
     * Returns Optional — meaning it might be empty if no user
     * is found with that email.
     *
     * Used during: Login (to find the user and check password)
     * SQL:         SELECT * FROM users WHERE email = ?
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with this email already exists.
     * Returns true or false.
     *
     * Used during: Registration (to prevent duplicate emails)
     * SQL:         SELECT COUNT(*) > 0 FROM users WHERE email = ?
     */
    boolean existsByEmail(String email);
}
