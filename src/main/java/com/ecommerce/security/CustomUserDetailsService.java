package com.ecommerce.security;

import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * ============================================================
 * CustomUserDetailsService.java — Bridges our User with Spring Security
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   Spring Security needs a "UserDetails" object to authenticate users.
 *   But our database has a "User" entity.
 *   This service acts as a BRIDGE — it loads our User from the
 *   database and converts it into a UserDetails object that
 *   Spring Security understands.
 *
 * HOW DOES IT WORK?
 *   1. User tries to log in with email + password.
 *   2. Spring Security calls loadUserByUsername(email).
 *   3. This method finds the User in the database.
 *   4. It wraps the User into a Spring Security User object
 *      that includes the email, encrypted password, and role.
 *   5. Spring Security then checks if the password matches.
 *
 * WHAT IS SimpleGrantedAuthority?
 *   It's how Spring Security represents a user's role/permission.
 *   We prefix with "ROLE_" because Spring Security expects it.
 *   Example: Role.ADMIN → "ROLE_ADMIN"
 *
 * WHY IMPLEMENT UserDetailsService?
 *   It's a Spring Security interface with one method:
 *   loadUserByUsername(). By implementing it, we tell Spring
 *   Security HOW to find users in our system.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Called by Spring Security when authenticating a user.
     *
     * @param email The user's email (we use email as the "username").
     * @return UserDetails object containing email, password, and role.
     * @throws UsernameNotFoundException if no user found with that email.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Step 1: Find the user in our database by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email));

        // Step 2: Convert our Role enum to a Spring Security authority
        //         e.g., Role.ADMIN → "ROLE_ADMIN"
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                "ROLE_" + user.getRole().name()
        );

        // Step 3: Create and return a Spring Security User object
        //         (this is org.springframework.security.core.userdetails.User,
        //          NOT our com.ecommerce.entity.User)
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),                           // username (we use email)
                user.getPassword(),                        // encrypted password
                Collections.singletonList(authority)        // list of roles/permissions
        );
    }
}
