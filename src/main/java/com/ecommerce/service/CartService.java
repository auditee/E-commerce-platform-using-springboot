package com.ecommerce.service;

import com.ecommerce.dto.AddToCartRequest;
import com.ecommerce.dto.CartItemResponse;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.UpdateCartItemRequest;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.CartItemRepository;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ============================================================
 * CartService.java — The Cart Business Logic Brain
 * ============================================================
 *
 * WHAT DOES THIS FILE DO?
 *   It implements the business logic for managing shopping carts:
 *     1. Finding/creating a user's cart.
 *     2. Adding products (checking stock availability, setting price/subtotal snapshots).
 *     3. Updating item quantities (with stock limits check).
 *     4. Removing individual items.
 *     5. Clearing the cart entirely.
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    /**
     * Gets or creates a cart for the user.
     */
    @Transactional
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .user(user)
                            .cartItems(new ArrayList<>())
                            .totalAmount(BigDecimal.ZERO)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    /**
     * Retrieves the cart for the logged-in user.
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));
        
        Optional<Cart> cartOpt = cartRepository.findByUser(user);
        if (cartOpt.isEmpty()) {
            // Return empty cart response as requested if cart doesn't exist
            return CartResponse.builder()
                    .cartId(null)
                    .userId(user.getId())
                    .items(new ArrayList<>())
                    .totalAmount(BigDecimal.ZERO)
                    .totalItems(0)
                    .build();
        }
        
        return mapToCartResponse(cartOpt.get());
    }

    /**
     * Adds a product to the user's cart.
     */
    @Transactional
    public CartResponse addToCart(AddToCartRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        // 1. Save the cart first if it does not exist yet to avoid unsaved transient parent issues
        Cart cart = cartRepository.findByUser(user).orElse(null);
        if (cart == null) {
            cart = Cart.builder()
                    .user(user)
                    .cartItems(new ArrayList<>())
                    .totalAmount(BigDecimal.ZERO)
                    .build();
            cart = cartRepository.save(cart);
        }

        // Initialize cart items if null
        if (cart.getCartItems() == null) {
            cart.setCartItems(new ArrayList<>());
        }

        // 2. Scan the collection directly to find the existing item (avoiding repository query on unsaved elements)
        CartItem existingItem = null;
        for (CartItem item : cart.getCartItems()) {
            if (item.getProduct().getId().equals(product.getId())) {
                existingItem = item;
                break;
            }
        }

        int targetQuantity = request.getQuantity();
        if (existingItem != null) {
            targetQuantity += existingItem.getQuantity();
        }

        // Verify stock limits (Do not reduce stock during cart operations!)
        if (product.getStockQuantity() < targetQuantity) {
            throw new RuntimeException("Requested quantity is not available in stock");
        }

        // 3. Create or update the item
        if (existingItem != null) {
            existingItem.setQuantity(targetQuantity);
            existingItem.setPrice(product.getPrice());
            existingItem.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(targetQuantity)));
        } else {
            CartItem newItem = CartItem.builder()
                    .product(product)
                    .quantity(request.getQuantity())
                    .price(product.getPrice())
                    .subtotal(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();
            // Use the helper method to attach CartItem and establish bidirectional relationship
            cart.addItem(newItem);
        }

        // 4. Recalculate cart totalAmount
        recalculateCartTotal(cart);

        // 5. Save the cart (cascading will save/update all cartItems automatically)
        Cart savedCart = cartRepository.save(cart);

        return mapToCartResponse(savedCart);
    }

    /**
     * Updates cart item quantity.
     */
    @Transactional
    public CartResponse updateCartItem(Long cartItemId, UpdateCartItemRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + user.getName()));

        CartItem cartItem = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        Product product = cartItem.getProduct();

        // Check product stock for the new quantity
        if (product.getStockQuantity() < request.getQuantity()) {
            throw new RuntimeException("Requested quantity is not available in stock");
        }

        // Update quantity and subtotal
        cartItem.setQuantity(request.getQuantity());
        cartItem.setSubtotal(cartItem.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));

        // Recalculate cart totalAmount and save cart (letting cascade handle cartItem save/update)
        recalculateCartTotal(cart);
        Cart savedCart = cartRepository.save(cart);

        return mapToCartResponse(savedCart);
    }

    /**
     * Removes an item from the user's cart.
     */
    @Transactional
    public void removeCartItem(Long cartItemId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + user.getName()));

        CartItem cartItem = cartItemRepository.findByIdAndCart(cartItemId, cart)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + cartItemId));

        // Use the helper method to remove item and clean up bidirectional relationship
        cart.removeItem(cartItem);
        
        // Recalculate totalAmount and save cart
        recalculateCartTotal(cart);
        cartRepository.save(cart);
    }

    /**
     * Clears all items inside the cart (searched by user email).
     */
    @Transactional
    public void clearCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + user.getName()));

        clearCart(cart);
    }

    /**
     * Clears all items inside the cart directly.
     */
    @Transactional
    public void clearCart(Cart cart) {
        cart.getCartItems().clear();
        cart.setTotalAmount(BigDecimal.ZERO);
        cartRepository.save(cart);
    }

    /**
     * Helper method to calculate the sum of subtotals for all items in the cart.
     */
    private void recalculateCartTotal(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getCartItems()) {
            total = total.add(item.getSubtotal());
        }
        cart.setTotalAmount(total);
    }

    /**
     * Helper method to map a Cart entity to a CartResponse DTO.
     */
    public CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getCartItems().stream()
                .map(item -> CartItemResponse.builder()
                        .cartItemId(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productImageUrl(item.getProduct().getImageUrl())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        int totalItems = cart.getCartItems().stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return CartResponse.builder()
                .cartId(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .totalAmount(cart.getTotalAmount())
                .totalItems(totalItems)
                .build();
    }
}
