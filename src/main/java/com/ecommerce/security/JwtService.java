package com.ecommerce.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ============================================================
 * JwtService.java — Creates and verifies JWT tokens
 * ============================================================
 *
 * WHAT IS A JWT TOKEN?
 *   JWT = JSON Web Token. It's a long encoded string like:
 *     eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2...
 *
 *   It has 3 parts separated by dots:
 *     1. Header  → Algorithm used (HS256)
 *     2. Payload → User data (email, issued time, expiry time)
 *     3. Signature → Encrypted hash to prove it's genuine
 *
 * HOW DOES IT WORK?
 *   1. User logs in with email + password.
 *   2. Server creates a JWT token and sends it back.
 *   3. Frontend saves the token (in localStorage or cookie).
 *   4. For every future request, frontend sends the token
 *      in the Authorization header:
 *        Authorization: Bearer eyJhbGci...
 *   5. Server reads the token, verifies it, and identifies
 *      the user WITHOUT checking the database every time.
 *
 * WHY IS IT SECURE?
 *   The token is signed with a SECRET KEY that only the
 *   server knows. If anyone tampers with the token, the
 *   signature won't match and the server will reject it.
 *
 * @Value reads values from application.properties:
 *   app.jwt.secret     → The secret key string
 *   app.jwt.expiration → How long the token lasts (milliseconds)
 */
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    // =========================================================
    // PUBLIC METHODS — Used by other classes
    // =========================================================

    /**
     * Extracts the username (email) from a JWT token.
     * The "subject" of a JWT is the user identifier — we use email.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generates a new JWT token for the given user.
     *
     * The token contains:
     *   - subject: user's email
     *   - issuedAt: current timestamp
     *   - expiration: current time + 24 hours
     *   - signature: signed with our secret key
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a token with extra custom data (claims).
     * For now we don't add extra data, but this method
     * allows it in the future (e.g., adding user role).
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)                                 // Extra data (empty for now)
                .subject(userDetails.getUsername())                   // User's email
                .issuedAt(new Date(System.currentTimeMillis()))      // Token creation time
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))  // Expiry time
                .signWith(getSigningKey())                           // Sign with secret key
                .compact();                                          // Build the token string
    }

    /**
     * Validates whether a token is legitimate and not expired.
     *
     * Checks two things:
     *   1. Does the email in the token match the given user?
     *   2. Has the token expired?
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // =========================================================
    // PRIVATE HELPER METHODS
    // =========================================================

    /**
     * Checks if the token has expired.
     * Compares the expiration date in the token with the current time.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from the token.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic method to extract any piece of data (claim) from the token.
     * Uses Java generics and functional interfaces — don't worry about
     * understanding this fully as a beginner, just know it "extracts data."
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Decodes the token and extracts all data (claims) from it.
     * If the token is tampered with or the signature doesn't match,
     * this method throws an exception.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())    // Use our secret key to verify
                .build()
                .parseSignedClaims(token)       // Parse and verify the token
                .getPayload();                  // Get the data inside
    }

    /**
     * Converts our secret key string into a cryptographic key object.
     *
     * The secret key in application.properties is a Base64-encoded string.
     * We decode it and create an HMAC-SHA key for signing/verifying tokens.
     */
    private SecretKey getSigningKey() {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            // Fallback: If configured secret is not valid Base64, read its raw string bytes.
            // HMAC-SHA256 requires key bytes to be at least 256 bits (32 bytes).
            byte[] keyBytes = secretKey.getBytes();
            if (keyBytes.length < 32) {
                byte[] padded = new byte[32];
                System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
                return Keys.hmacShaKeyFor(padded);
            }
            return Keys.hmacShaKeyFor(keyBytes);
        }
    }
}
