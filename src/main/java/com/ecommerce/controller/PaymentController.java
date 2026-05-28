package com.ecommerce.controller;

import com.ecommerce.dto.PaymentRequest;
import com.ecommerce.dto.PaymentResponse;
import com.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 * PaymentController.java — REST endpoints for simulated payments
 * ============================================================
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Process simulated payment
     * POST /api/payments/process
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        PaymentResponse response = paymentService.processPayment(request, userEmail);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment details for an order
     * GET /api/payments/order/{orderId}
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentDetails(
            @PathVariable Long orderId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        PaymentResponse response = paymentService.getPaymentDetails(orderId, userEmail);
        return ResponseEntity.ok(response);
    }
}
