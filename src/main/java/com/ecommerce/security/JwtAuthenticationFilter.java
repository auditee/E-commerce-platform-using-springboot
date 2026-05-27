package com.ecommerce.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ============================================================
 * JwtAuthenticationFilter.java — The security checkpoint
 * ============================================================
 *
 * WHAT IS A FILTER?
 *   A filter is like a security checkpoint at the airport.
 *   EVERY request that comes into our app passes through
 *   this filter BEFORE reaching the controller.
 *
 * WHAT DOES THIS FILTER DO?
 *   1. Checks if the request has an "Authorization" header.
 *   2. If yes, extracts the JWT token from it.
 *   3. Validates the token (is it genuine? is it expired?).
 *   4. If valid, tells Spring Security "this user is authenticated."
 *   5. The request then continues to the controller.
 *
 * WHAT IS OncePerRequestFilter?
 *   It guarantees this filter runs exactly ONCE per request.
 *   (Without it, some requests might get filtered multiple times.)
 *
 * THE AUTHORIZATION HEADER FORMAT:
 *   Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
 *   ──────────── ──────── ───────────────────────
 *    Header name   Prefix        JWT token
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // ─── Step 1: Get the Authorization header ───
        final String authHeader = request.getHeader("Authorization");

        // If there's no Authorization header, or it doesn't start with "Bearer ",
        // skip this filter and let the request continue (it might be a public endpoint).
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // ─── Step 2: Extract the token (remove "Bearer " prefix) ───
        // "Bearer eyJhbGci..." → "eyJhbGci..."
        final String jwt = authHeader.substring(7);

        // ─── Step 3: Extract the email from the token ───
        final String userEmail;
        try {
            userEmail = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            // If token is malformed or tampered with, skip authentication
            filterChain.doFilter(request, response);
            return;
        }

        // ─── Step 4: If email exists and user is NOT already authenticated ───
        if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Load the user from the database
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

            // ─── Step 5: Validate the token ───
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // ─── Step 6: Tell Spring Security "this user is authenticated" ───
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,                     // The authenticated user
                                null,                            // No credentials needed (token is proof)
                                userDetails.getAuthorities()     // The user's roles (ROLE_USER, ROLE_ADMIN)
                        );

                // Attach extra details about the request (like IP address)
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Save the authentication in Spring Security's context
                // From this point, Spring Security knows WHO this user is
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // ─── Step 7: Continue the filter chain (let the request proceed) ───
        filterChain.doFilter(request, response);
    }
}
