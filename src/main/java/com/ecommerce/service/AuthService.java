package com.ecommerce.service;

import com.ecommerce.dto.AuthResponse;
import com.ecommerce.dto.LoginRequest;
import com.ecommerce.dto.RegisterRequest;
import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import com.ecommerce.repository.UserRepository;
import com.ecommerce.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * ============================================================
 * AuthService.java — Registration and login logic
 * ============================================================
 *
 * This is the "brain" for authentication.
 * It handles two operations:
 *   1. Register: Create a new user account.
 *   2. Login: Verify credentials and return a JWT token.
 *
 * REGISTRATION FLOW:
 *   Frontend sends: { name, email, password, role }
 *   ↓
 *   Check if email already exists → if yes, throw error.
 *   ↓
 *   Encrypt the password using BCrypt.
 *   ↓
 *   Save user to database.
 *   ↓
 *   Generate JWT token.
 *   ↓
 *   Return AuthResponse with token + user info.
 *
 * LOGIN FLOW:
 *   Frontend sends: { email, password }
 *   ↓
 *   AuthenticationManager checks credentials.
 *   ↓
 *   If invalid → Spring Security throws BadCredentialsException.
 *   ↓
 *   If valid → Generate JWT token.
 *   ↓
 *   Return AuthResponse with token + user info.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // =========================================================
    // REGISTER — Create a new user account
    // =========================================================
    public AuthResponse register(RegisterRequest request) {

        // Step 1: Check if email is already taken
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }

        // Step 2: Determine the role
        // If the user didn't specify a role, default to USER
        Role role = Role.USER;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                role = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid role: " + request.getRole()
                        + ". Allowed values: USER, ADMIN");
            }
        }

        // Step 3: Create the User entity
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))  // Encrypt!
                .role(role)
                .build();

        // Step 4: Save to database
        User savedUser = userRepository.save(user);

        // Step 5: Generate JWT token
        // We need a UserDetails object for the JwtService
        var userDetails = new org.springframework.security.core.userdetails.User(
                savedUser.getEmail(),
                savedUser.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + savedUser.getRole().name()))
        );
        String token = jwtService.generateToken(userDetails);

        // Step 6: Build and return the response
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    // =========================================================
    // LOGIN — Authenticate and return JWT token
    // =========================================================
    public AuthResponse login(LoginRequest request) {

        // Step 1: Authenticate the user
        // This automatically:
        //   a) Calls CustomUserDetailsService.loadUserByUsername()
        //   b) Compares the entered password with the stored BCrypt hash
        //   c) Throws BadCredentialsException if wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Step 2: If we reach here, credentials are valid!
        // Find the user in the database
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Step 3: Generate JWT token
        var userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        String token = jwtService.generateToken(userDetails);

        // Step 4: Return the response
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
