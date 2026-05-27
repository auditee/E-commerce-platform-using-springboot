package com.ecommerce.controller;

import com.ecommerce.dto.ProductRequest;
import com.ecommerce.dto.ProductResponse;
import com.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ============================================================
 * ProductController.java — The REST API entry point
 * ============================================================
 *
 * WHAT IS A CONTROLLER?
 *   A controller is the "front door" of our backend.
 *   It receives HTTP requests from the frontend (or Postman)
 *   and sends back HTTP responses.
 *
 * HOW DOES A REQUEST FLOW?
 *   1. User sends:      POST http://localhost:8080/api/products
 *   2. Controller:       Receives the request, validates the body.
 *   3. Service:          Processes the business logic.
 *   4. Repository:       Saves data to the database.
 *   5. Controller:       Sends back a JSON response.
 *
 * ANNOTATIONS EXPLAINED:
 *   @RestController    → Combines @Controller + @ResponseBody.
 *                        Tells Spring "this class handles HTTP requests
 *                        and returns JSON responses (not HTML pages)."
 *   @RequestMapping    → Sets the base URL. All endpoints in this
 *                        controller start with /api/products.
 *   @RequiredArgsConstructor → Lombok injects the ProductService
 *                              automatically via the constructor.
 *
 * HTTP METHODS EXPLAINED:
 *   @PostMapping       → Handles POST requests (CREATE something new).
 *   @GetMapping        → Handles GET requests (READ / fetch data).
 *   @PutMapping        → Handles PUT requests (UPDATE existing data).
 *   @DeleteMapping     → Handles DELETE requests (REMOVE data).
 *
 * OTHER ANNOTATIONS:
 *   @RequestBody       → Tells Spring to read the JSON body of the
 *                        request and convert it to a Java object.
 *   @Valid             → Activates the validation annotations in the DTO
 *                        (like @NotBlank, @Positive). If validation fails,
 *                        Spring automatically returns a 400 error.
 *   @PathVariable      → Extracts a value from the URL.
 *                        e.g. /api/products/5 → id = 5
 *   ResponseEntity     → Lets us set the HTTP status code
 *                        (201 Created, 200 OK, 204 No Content).
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    // Spring injects the ProductService here automatically
    private final ProductService productService;

    // =========================================================
    // POST /api/products — Create a new product
    // =========================================================
    /**
     * Creates a new product in the database.
     * @Valid ensures the request body is validated before processing.
     * Returns HTTP 201 (CREATED) on success.
     *
     * Example request body (JSON):
     * {
     *   "name": "iPhone 15",
     *   "description": "Apple iPhone 15 with 128GB storage",
     *   "price": 79999.00,
     *   "stockQuantity": 10,
     *   "category": "Mobile",
     *   "imageUrl": "https://example.com/iphone15.jpg"
     * }
     */
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.createProduct(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);  // 201
    }

    // =========================================================
    // GET /api/products — Get all products
    // =========================================================
    /**
     * Returns a list of ALL products in the database.
     * Returns HTTP 200 (OK) on success.
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {

        List<ProductResponse> products = productService.getAllProducts();
        return ResponseEntity.ok(products);  // 200
    }

    // =========================================================
    // GET /api/products/{id} — Get a single product by ID
    // =========================================================
    /**
     * Returns one specific product by its ID.
     * If the product doesn't exist, returns HTTP 404.
     *
     * @PathVariable extracts {id} from the URL.
     * Example: GET /api/products/3 → id = 3
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @PathVariable Long id) {

        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(response);  // 200
    }

    // =========================================================
    // PUT /api/products/{id} — Update an existing product
    // =========================================================
    /**
     * Updates an existing product with new data.
     * If the product doesn't exist, returns HTTP 404.
     * Returns HTTP 200 (OK) on success.
     *
     * Example request body (JSON):
     * {
     *   "name": "iPhone 15 Pro",
     *   "description": "Apple iPhone 15 Pro with 256GB storage",
     *   "price": 129999.00,
     *   "stockQuantity": 5,
     *   "category": "Mobile",
     *   "imageUrl": "https://example.com/iphone15pro.jpg"
     * }
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);  // 200
    }

    // =========================================================
    // DELETE /api/products/{id} — Delete a product
    // =========================================================
    /**
     * Deletes a product by its ID.
     * If the product doesn't exist, returns HTTP 404.
     * Returns HTTP 204 (NO CONTENT) on success — meaning
     * "the action was successful but there's nothing to send back."
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {

        productService.deleteProduct(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);  // 204
    }
}
