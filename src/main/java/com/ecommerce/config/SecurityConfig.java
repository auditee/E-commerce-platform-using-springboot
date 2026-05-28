package com.ecommerce.config;

import com.ecommerce.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ============================================================
 * SecurityConfig.java — The master security control panel
 * ============================================================
 *
 * This file replaces the temporary "allow everything" config
 * from Phase 1. Now it has REAL security rules.
 *
 * WHAT DOES THIS FILE CONFIGURE?
 *   1. WHICH APIs are public (no login needed)?
 *   2. WHICH APIs require login?
 *   3. WHICH APIs require ADMIN role?
 *   4. HOW to encrypt passwords (BCrypt)?
 *   5. WHERE to plug in our JWT filter?
 *   6. HOW to authenticate users (using our CustomUserDetailsService)?
 *
 * SECURITY RULES SUMMARY:
 *   ┌──────────────────────────────┬────────────────────────┐
 *   │ API                          │ Who can access?        │
 *   ├──────────────────────────────┼────────────────────────┤
 *   │ POST /api/auth/register      │ Everyone (public)      │
 *   │ POST /api/auth/login         │ Everyone (public)      │
 *   │ GET  /api/products           │ Everyone (public)      │
 *   │ GET  /api/products/{id}      │ Everyone (public)      │
 *   │ POST /api/products           │ ADMIN only             │
 *   │ PUT  /api/products/{id}      │ ADMIN only             │
 *   │ DELETE /api/products/{id}    │ ADMIN only             │
 *   │ All other APIs               │ Logged-in users        │
 *   └──────────────────────────────┴────────────────────────┘
 *
 * WHAT IS STATELESS SESSION?
 *   Traditional websites use cookies + sessions (the server
 *   remembers who you are). Our REST API uses JWT tokens instead.
 *   "Stateless" means the server does NOT remember anything —
 *   every request must carry its own JWT token as proof.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * The main security rules of our application.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── 1. Disable CSRF ──
            // CSRF protection is for browser-based form submissions.
            // Our REST API uses JWT tokens instead, so CSRF is not needed.
            .csrf(AbstractHttpConfigurer::disable)

            // ── 2. Define which URLs are public, which need auth ──
            .authorizeHttpRequests(auth -> auth
                // PUBLIC: Anyone can register and login
                .requestMatchers("/api/auth/**").permitAll()

                // PUBLIC: Anyone can VIEW products (GET requests only)
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()

                // ADMIN ONLY: Only admins can CREATE, UPDATE, DELETE products
                .requestMatchers(HttpMethod.POST, "/api/products", "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/products", "/api/products/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products", "/api/products/**").hasRole("ADMIN")

                // CART, ORDERS & PAYMENTS: Must be logged in (accessible by both USER and ADMIN)
                .requestMatchers("/api/cart", "/api/cart/**").authenticated()
                .requestMatchers("/api/orders", "/api/orders/**").authenticated()
                .requestMatchers("/api/payments", "/api/payments/**").authenticated()

                // ALL OTHER REQUESTS: Must be logged in (any role)
                .anyRequest().authenticated()
            )

            // ── 3. Use stateless sessions (no cookies, JWT only) ──
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ── 4. Use our custom authentication provider ──
            .authenticationProvider(authenticationProvider())

            // ── 5. Add our JWT filter BEFORE Spring's default login filter ──
            // This ensures every request is checked for a JWT token first.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Password Encoder — BCrypt
     *
     * BCrypt is a one-way hashing algorithm.
     * "password123" → "$2a$10$dXJ3SW6G7P50lGmMQoeaiu..."
     *
     * It CANNOT be reversed. Even we (the developers) cannot
     * see the real password. During login, BCrypt hashes the
     * entered password and compares the hashes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication Provider — connects our UserDetailsService + PasswordEncoder.
     *
     * When a user logs in:
     *   1. DaoAuthenticationProvider uses our CustomUserDetailsService
     *      to load the user from the database.
     *   2. It uses BCryptPasswordEncoder to compare the entered
     *      password with the stored hash.
     *   3. If they match → authentication successful.
     *   4. If they don't match → authentication fails.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Authentication Manager — used to trigger authentication.
     *
     * Our AuthService calls authenticationManager.authenticate()
     * when a user logs in. This kicks off the whole process
     * (find user → check password → return result).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
