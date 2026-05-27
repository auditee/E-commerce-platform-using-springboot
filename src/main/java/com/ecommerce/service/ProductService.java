package com.ecommerce.service;

import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ============================================================
 * ProductService.java — Business logic + Redis Caching
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
 * WHAT IS CACHING?
 *   Caching means storing the result of an expensive operation
 *   so we can return it faster the next time.
 *
 *   Example without caching:
 *     User A calls GET /api/products → hits MySQL → 80ms
 *     User B calls GET /api/products → hits MySQL → 80ms
 *     User C calls GET /api/products → hits MySQL → 80ms
 *     (MySQL gets 3 queries, even for the same data!)
 *
 *   Example WITH caching (Redis):
 *     User A calls GET /api/products → hits MySQL → 80ms
 *                                     → stores result in Redis
 *     User B calls GET /api/products → hits Redis → 2ms  (instant!)
 *     User C calls GET /api/products → hits Redis → 2ms  (instant!)
 *     (MySQL only gets 1 query. Redis serves the rest instantly!)
 *
 * CACHING ANNOTATIONS EXPLAINED:
 *
 *   @Cacheable(cacheNames = "products")
 *     Before running this method, Spring checks Redis first:
 *       - Cache HIT  → data is in Redis → return it immediately,
 *                       method body is NEVER executed.
 *       - Cache MISS → data not in Redis → run method, get result
 *                       from MySQL, store it in Redis, return it.
 *
 *   @Cacheable(cacheNames = "product", key = "#id")
 *     Same idea, but for a single product. The key includes the
 *     product ID, so each product gets its own Redis slot:
 *     "product::1", "product::2", "product::42", etc.
 *
 *   @CacheEvict(cacheNames = "...", allEntries = true)
 *     Removes all entries from the named cache.
 *     Used when product data changes (create/update/delete).
 *
 *   WHY DO WE EVICT (CLEAR) THE CACHE ON WRITE OPERATIONS?
 *     Imagine Redis has cached: "iPhone 15 — price Rs.79,999"
 *     An admin updates the price to Rs.69,999 in MySQL.
 *     If we don't clear the cache, the next GET request will
 *     still show Rs.79,999 (stale data from Redis — wrong!).
 *     By evicting on every write, the next GET request will go
 *     to MySQL, get the fresh data, and re-populate Redis.
 *
 *   @Caching(evict = { ... })
 *     Allows combining multiple @CacheEvict on one method.
 *     For update/delete, we clear BOTH caches:
 *       - "products" (the cached list of all products)
 *       - "product"  (the cached single product by ID)
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
     * Creates a new product and evicts the "products" list cache.
     *
     * WHY EVICT "products"?
     *   After adding a new product, the cached list of all products
     *   is outdated (it's missing the new product). We clear it so
     *   the next GET /api/products will re-fetch the full list from
     *   MySQL and update the cache with the newly added product.
     */
    @CacheEvict(cacheNames = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        // Step 1: Convert the request DTO to a Product entity
        Product product = mapToEntity(request);

        // Step 2: Save the entity to the database
        //         The repository's .save() method does an INSERT in SQL.
        //         After saving, the 'product' object has an auto-generated ID.
        Product savedProduct = productRepository.save(product);

        // Step 3: Convert the saved entity to a response DTO and return it
        return mapToResponse(savedProduct);
    }

    // =========================================================
    // READ ALL — Get all products from the database
    // =========================================================

    /**
     * Returns all products. Results are cached in Redis under
     * the key "products".
     *
     * WHAT HAPPENS:
     *   1st call → Redis is empty (cache MISS).
     *              Method runs, hits MySQL, returns the full list.
     *              Spring also stores the list in Redis automatically.
     *   2nd call → Redis has the data (cache HIT).
     *              Method body is SKIPPED entirely.
     *              Spring returns the cached list instantly.
     *
     * .stream() = Process each item in the list one by one.
     * .map()    = Convert each Product entity to a ProductResponse DTO.
     * .collect()= Gather all results into a new List.
     */
    @Cacheable(cacheNames = "products")
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
     * Returns a single product by ID. Cached in Redis under
     * "product::{id}" (e.g. "product::1", "product::5").
     *
     * key = "#id" means the cache key is dynamic — it uses
     * the value of the 'id' parameter passed to this method.
     *
     * WHAT HAPPENS:
     *   GET /api/products/3 → checks "product::3" in Redis.
     *   If found → return from cache instantly.
     *   If not   → query MySQL, cache result as "product::3", return it.
     *
     * .orElseThrow() means: return the product if found,
     * or throw ResourceNotFoundException (which gives a 404 response).
     */
    @Cacheable(cacheNames = "product", key = "#id")
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
     * Updates an existing product and evicts BOTH caches.
     *
     * WHY EVICT BOTH "products" AND "product"?
     *   - "products" cache has the list of all products.
     *     If we updated a price, the list is now stale. Clear it.
     *   - "product::{id}" has the individual product that changed.
     *     It's also stale. Clear it.
     *
     * @Caching groups multiple @CacheEvict on one method.
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = "products", allEntries = true),
            @CacheEvict(cacheNames = "product", key = "#id")
    })
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        // Step 1: Find the existing product (or throw 404 if not found)
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        // Step 2: Update the fields with new values from the request
        existingProduct.setName(request.getName());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setPrice(request.getPrice());
        existingProduct.setStockQuantity(request.getStockQuantity());
        existingProduct.setCategory(request.getCategory());
        existingProduct.setImageUrl(request.getImageUrl());

        // Step 3: Save the updated entity.
        //         Because the entity already has an ID, .save() does
        //         an UPDATE in SQL (not an INSERT).
        Product updatedProduct = productRepository.save(existingProduct);

        // Step 4: Return the updated response
        return mapToResponse(updatedProduct);
    }

    // =========================================================
    // DELETE — Remove a product from the database
    // =========================================================

    /**
     * Deletes a product and evicts BOTH caches.
     *
     * WHY EVICT BOTH "products" AND "product"?
     *   After deleting, the product no longer exists in MySQL.
     *   If "product::5" is still cached, someone could request
     *   a deleted product and get stale data back — wrong!
     *   Evicting both caches ensures deleted data is gone everywhere.
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = "products", allEntries = true),
            @CacheEvict(cacheNames = "product", key = "#id")
    })
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
     * Used when SENDING data back to the client.
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
