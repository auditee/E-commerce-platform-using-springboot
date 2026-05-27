package com.ecommerce.controller;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 * CartController.java — The Cart REST Endpoint Controller
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It exposes REST endpoints for cart actions:
 *     - POST /api/cart/add (Add product to cart)
 *     - GET /api/cart (View logged-in user's cart)
 *     - PUT /api/cart/item/{cartItemId} (Update item quantity)
 *     - DELETE /api/cart/item/{cartItemId} (Remove item)
 *     - DELETE /api/cart/clear (Clear full cart)
 *   Identifies the user using the JWT authentication token securely.
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * View logged-in user's cart
     * GET /api/cart
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        String userEmail = authentication.getName();
        CartResponse cartResponse = cartService.getCart(userEmail);
        return ResponseEntity.ok(cartResponse);
    }

    /**
     * Add product to cart
     * POST /api/cart/add
     */
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        CartResponse cartResponse = cartService.addToCart(request, userEmail);
        return ResponseEntity.ok(cartResponse);
    }

    /**
     * Update cart item quantity
     * PUT /api/cart/item/{cartItemId}
     */
    @PutMapping("/item/{cartItemId}")
    public ResponseEntity<CartResponse> updateCartItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        CartResponse cartResponse = cartService.updateCartItem(cartItemId, request, userEmail);
        return ResponseEntity.ok(cartResponse);
    }

    /**
     * Remove item from cart
     * DELETE /api/cart/item/{cartItemId}
     */
    @DeleteMapping("/item/{cartItemId}")
    public ResponseEntity<Void> removeCartItem(
            @PathVariable Long cartItemId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        cartService.removeCartItem(cartItemId, userEmail);
        return ResponseEntity.ok().build();
    }

    /**
     * Clear full cart
     * DELETE /api/cart/clear
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        String userEmail = authentication.getName();
        cartService.clearCart(userEmail);
        return ResponseEntity.ok().build();
    }
}
