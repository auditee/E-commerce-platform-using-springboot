package com.ecommerce.service;

import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * ProductService.java — The business logic layer
 * ============================================================
 *
 * WHAT IS A SERVICE?
 *   The service contains the "brain" or business logic.
 *   It sits between the Controller and the Repository:
 *
 *   Controller → Service → Repository → Database
 *
 *   The Controller receives the request.
 *   The Service does the thinking (validation, conversion, rules).
 *   The Repository does the database work (save, find, delete).
 *
 * WHY NOT PUT LOGIC IN THE CONTROLLER?
 *   Keeping logic in the Service makes our code:
 *   1. Reusable — multiple controllers can share the same service.
 *   2. Testable — we can test the logic without starting a web server.
 *   3. Clean — each layer has one clear job.
 *
 * ANNOTATIONS EXPLAINED:
 *   @Service → Tells Spring "this class is a service, manage it for me."
 *   @RequiredArgsConstructor → Lombok creates a constructor that injects
 *     all 'final' fields. This is called "constructor injection" — the
 *     recommended way to connect classes in Spring.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    // Spring automatically injects the ProductRepository here
    // because of @RequiredArgsConstructor + final keyword.
    private final ProductRepository productRepository;

    // =========================================================
    // CREATE — Save a new product to the database
    // =========================================================
    /**
     * Takes the data from ProductRequest, converts it to a Product
     * entity, saves it to the database, and returns a ProductResponse.
     *
     * Flow: ProductRequest → Product (entity) → save → ProductResponse
     */
    public ProductResponse createProduct(ProductRequest request) {
        // Step 1: Convert the request DTO to a Product entity
        Product product = mapToEntity(request);

        // Step 2: Save the entity to the database
        //         The repository's .save() method does an INSERT in SQL.
        //         After saving, the 'product' object now has an auto-generated ID.
        Product savedProduct = productRepository.save(product);

        // Step 3: Convert the saved entity to a response DTO and return it
        return mapToResponse(savedProduct);
    }

    // =========================================================
    // READ ALL — Get all products from the database
    // =========================================================
    /**
     * Fetches every product from the database and converts each
     * one to a ProductResponse.
     *
     * .stream() = Process each item in the list one by one.
     * .map()    = Convert each Product to a ProductResponse.
     * .collect() = Gather all results into a new List.
     */
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // =========================================================
    // READ ONE — Get a single product by its ID
    // =========================================================
    /**
     * Finds a product by ID. If it doesn't exist, throws our
     * custom ResourceNotFoundException (which returns a 404 error).
     *
     * .orElseThrow() means: "If the product is found, return it.
     * If not found, throw this exception."
     */
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        return mapToResponse(product);
    }

    // =========================================================
    // UPDATE — Modify an existing product
    // =========================================================
    /**
     * Finds the existing product by ID, updates its fields with
     * the new data from ProductRequest, saves it, and returns
     * the updated ProductResponse.
     */
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        // Step 1: Find the existing product (or throw 404)
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        // Step 2: Update the fields with new values
        existingProduct.setName(request.getName());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setStockQuantity(request.getStockQuantity());
        existingProduct.setCategory(request.getCategory());
        existingProduct.setImageUrl(request.getImageUrl());

        // Step 3: Save the updated entity.
        //         Because the entity already has an ID, .save() does
        //         an UPDATE (not an INSERT).
        Product updatedProduct = productRepository.save(existingProduct);

        // Step 4: Return the updated response
        return mapToResponse(updatedProduct);
    }

    // =========================================================
    // DELETE — Remove a product from the database
    // =========================================================
    /**
     * Deletes a product by ID. First checks if it exists.
     * If not, throws a 404 error.
     */
    public void deleteProduct(Long id) {
        // Check if the product exists before trying to delete
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        productRepository.delete(product);
    }

    // =========================================================
    // HELPER METHODS — Convert between Entity and DTO
    // =========================================================

    /**
     * Converts a ProductRequest DTO → Product Entity.
     * Used when CREATING a new product.
     * We use the @Builder pattern from Lombok for clean code.
     */
    private Product mapToEntity(ProductRequest request) {
        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .build();
    }

    /**
     * Converts a Product Entity → ProductResponse DTO.
     * Used when SENDING data back to the frontend.
     */
    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .category(product.getCategory())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
