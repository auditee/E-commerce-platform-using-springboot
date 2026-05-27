package com.ecommerce.controller;

import com.ecommerce.dto.OrderResponse;
import com.ecommerce.dto.PlaceOrderRequest;
import com.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * OrderController.java — The Order REST Endpoint Controller
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It exposes REST endpoints for placing orders and viewing order history.
 *   Only authenticated users can access these APIs (enforced by SecurityConfig).
 *   We identify the user via their JWT token (from Authentication object),
 *   never by passing a userId in the request body or path.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Place order from cart
     * POST /api/orders/place
     */
    @PostMapping("/place")
    public ResponseEntity<OrderResponse> placeOrder(
            @Valid @RequestBody PlaceOrderRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        OrderResponse orderResponse = orderService.placeOrder(request, userEmail);
        return new ResponseEntity<>(orderResponse, HttpStatus.CREATED);
    }

    /**
     * View logged-in user's orders
     * GET /api/orders/my-orders
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderResponse>> getMyOrders(Authentication authentication) {
        String userEmail = authentication.getName();
        List<OrderResponse> orders = orderService.getMyOrders(userEmail);
        return ResponseEntity.ok(orders);
    }

    /**
     * View single order by ID
     * GET /api/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        OrderResponse orderResponse = orderService.getOrderById(orderId, userEmail);
        return ResponseEntity.ok(orderResponse);
    }
}
