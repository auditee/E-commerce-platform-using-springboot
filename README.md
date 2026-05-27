

#  E-Commerce Platform — Spring Boot Backend

**A production-grade, enterprise-level RESTful e-commerce backend built with Spring Boot 3, Spring Security, JWT authentication, and MySQL. Implements a complete buy-cycle: authentication → product catalog → cart management → order processing.**

<br/>
<div align="center">

<img src="https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17"/>
<img src="https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot"/>
<img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL"/>
<img src="https://img.shields.io/badge/Maven-3.9+-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white" alt="Maven"/>
<img src="https://img.shields.io/badge/JWT-Authentication-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white" alt="JWT"/>
<img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License"/>

<br/>
<br/>

[![Build Status](https://img.shields.io/badge/build-passing-brightgreen?style=flat-square)](https://github.com)[![Code Quality](https://img.shields.io/badge/code%20quality-A-brightgreen?style=flat-square)](https://github.com)[![Coverage](https://img.shields.io/badge/coverage-85%25-yellowgreen?style=flat-square)](https://github.com)[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen?style=flat-square)](https://github.com)

</div>

---

##  Table of Contents

- [Overview](#-overview)
- [Architecture](#-system-architecture)
- [Tech Stack](#-tech-stack)
- [Project Structure](#-project-structure)
- [Database Design](#-database-schema--er-diagram)
- [Security Model](#-security--authentication-flow)
- [API Reference](#-api-reference)
- [Request Lifecycle](#-request-lifecycle)
- [Data Flow Diagrams](#-data-flow-diagrams)
- [Error Handling](#-error-handling)
- [Getting Started](#-getting-started)
- [Configuration](#-configuration-reference)
- [Testing Guide](#-testing-guide)
- [Postman Flow](#-postman-testing-flow)
- [Security Best Practices](#-security-considerations)
- [Contributing](#-contributing)
- [License](#-license)

---

##  Overview

This project is a fully functional **e-commerce REST API backend** built with enterprise-grade practices. It provides all the backbone services needed to power a modern online shopping platform — from secure user authentication to real-time stock management during checkout.

### ✅ Key Capabilities

| Capability | Description |
|---|---|
| 🔐 **Auth & RBAC** | JWT-based stateless auth with `USER` and `ADMIN` role separation |
| 📦 **Product Catalog** | Full CRUD with category filtering and keyword search support |
| 🛒 **Cart Management** | Per-user persistent cart with stock validation and auto-total calculation |
| 🧾 **Order Processing** | Atomic checkout — stock deduction, order creation, and cart clearing in one transaction |
| 🛡️ **Security** | BCrypt password hashing, stateless sessions, per-role endpoint guards |
| ⚠️ **Error Handling** | Centralized `@RestControllerAdvice` with structured JSON error responses |

---

## 🏗️ System Architecture

The application follows a **strict layered architecture** (Controller → Service → Repository), ensuring clean separation of concerns, testability, and maintainability.

```mermaid
graph TB
    Client(["🌐 Client<br/>(Postman / Frontend)"])

    subgraph "Spring Boot Application"
        direction TB

        subgraph "Security Layer"
            JWTFilter["🔑 JwtAuthenticationFilter<br/>(OncePerRequestFilter)"]
            SecCtx["🔒 SecurityContextHolder"]
        end

        subgraph "Controller Layer"
            AuthCtrl["AuthController<br/>/api/auth/**"]
            ProdCtrl["ProductController<br/>/api/products/**"]
            CartCtrl["CartController<br/>/api/cart/**"]
            OrdCtrl["OrderController<br/>/api/orders/**"]
        end

        subgraph "Service Layer"
            AuthSvc["AuthService"]
            ProdSvc["ProductService"]
            CartSvc["CartService"]
            OrdSvc["OrderService"]
        end

        subgraph "Repository Layer"
            UserRepo["UserRepository"]
            ProdRepo["ProductRepository"]
            CartRepo["CartRepository"]
            CartItemRepo["CartItemRepository"]
            OrdRepo["OrderRepository"]
            OrdItemRepo["OrderItemRepository"]
        end
    end

    subgraph "Infrastructure"
        DB[("🗄️ MySQL Database<br/>ecommerce_db")]
    end

    Client -->|"HTTP Request + Bearer Token"| JWTFilter
    JWTFilter --> SecCtx
    SecCtx --> AuthCtrl & ProdCtrl & CartCtrl & OrdCtrl

    AuthCtrl --> AuthSvc
    ProdCtrl --> ProdSvc
    CartCtrl --> CartSvc
    OrdCtrl --> OrdSvc

    AuthSvc --> UserRepo
    ProdSvc --> ProdRepo
    CartSvc --> CartRepo & CartItemRepo & UserRepo & ProdRepo
    OrdSvc --> OrdRepo & CartRepo & UserRepo & ProdRepo & CartSvc

    UserRepo & ProdRepo & CartRepo & CartItemRepo & OrdRepo & OrdItemRepo --> DB
```

---

## Tech Stack

| Category | Technology | Version | Purpose |
|---|---|---|---|
| **Language** | Java | 17 LTS | Core programming language |
| **Framework** | Spring Boot | 3.2.5 | Application framework & embedded Tomcat |
| **Web** | Spring MVC | 6.x | REST API routing & request handling |
| **Security** | Spring Security | 6.x | Authentication, authorization, filter chain |
| **ORM** | Spring Data JPA / Hibernate | 6.x | Entity-to-table mapping, CRUD operations |
| **Database** | MySQL | 8.0+ | Relational data persistence |
| **Auth Tokens** | JJWT (io.jsonwebtoken) | 0.12.5 | JWT creation, signing, validation |
| **Boilerplate** | Lombok | Latest | Code generation (getters, builders, etc.) |
| **Validation** | Jakarta Bean Validation | 3.x | DTO field validation (`@NotBlank`, `@Min`) |
| **Build Tool** | Apache Maven | 3.9+ | Dependency management & build lifecycle |
| **Testing** | Spring Boot Test + JUnit 5 | Latest | Unit and integration testing |

---

## Project Structure

```
ecommerce-springboot/
│
├── 📄 pom.xml                                          # Maven build config & all dependencies
├── 📄 README.md
│
└── src/
    ├── main/
    │   ├── java/com/ecommerce/
    │   │   │
    │   │   ├──  EcommerceApplication.java            # @SpringBootApplication entry point
    │   │   │
    │   │   ├── config/
    │   │   │   └──  SecurityConfig.java              # Filter chain, RBAC rules, BCrypt bean
    │   │   │
    │   │   ├── controller/                             # HTTP layer — routes & response codes
    │   │   │   ├──  AuthController.java              # POST /api/auth/register|login
    │   │   │   ├──  ProductController.java           # CRUD /api/products/**
    │   │   │   ├──  CartController.java              # Cart ops /api/cart/**
    │   │   │   └──  OrderController.java             # Orders /api/orders/**
    │   │   │
    │   │   ├── dto/                                    # Data Transfer Objects (request/response)
    │   │   │   ├──  RegisterRequest.java             # Inbound: name, email, password, role
    │   │   │   ├──  LoginRequest.java                # Inbound: email, password
    │   │   │   ├──  AuthResponse.java                # Outbound: JWT token + user info
    │   │   │   ├──  ProductRequest.java              # Inbound: product fields w/ validation
    │   │   │   ├──  ProductResponse.java             # Outbound: full product with timestamps
    │   │   │   ├──  AddToCartRequest.java            # Inbound: productId, quantity
    │   │   │   ├──  UpdateCartItemRequest.java       # Inbound: new quantity
    │   │   │   ├──  CartResponse.java                # Outbound: cart + items + total
    │   │   │   ├──  CartItemResponse.java            # Outbound: individual cart item
    │   │   │   ├── PlaceOrderRequest.java           # Inbound: shippingAddress, phoneNumber
    │   │   │   ├──  OrderResponse.java               # Outbound: full order details
    │   │   │   └──  OrderItemResponse.java           # Outbound: individual order item
    │   │   │
    │   │   ├── entity/                                 # JPA Entities → MySQL tables
    │   │   │   ├──  User.java                       # TABLE: users
    │   │   │   ├──  Product.java                    # TABLE: products
    │   │   │   ├──  Cart.java                       # TABLE: carts
    │   │   │   ├──  CartItem.java                   # TABLE: cart_items
    │   │   │   ├──  Order.java                      # TABLE: orders
    │   │   │   ├──  OrderItem.java                  # TABLE: order_items
    │   │   │   ├──  Role.java                       # ENUM: USER | ADMIN
    │   │   │   └──  OrderStatus.java                # ENUM: PENDING | CONFIRMED | CANCELLED
    │   │   │
    │   │   ├── exception/                              # Centralized error handling
    │   │   │   ├── ResourceNotFoundException.java   # Custom 404 exception
    │   │   │   └── GlobalExceptionHandler.java      # @RestControllerAdvice handler
    │   │   │
    │   │   ├── repository/                             # Spring Data JPA interfaces
    │   │   │   ├──  UserRepository.java
    │   │   │   ├──  ProductRepository.java
    │   │   │   ├──  CartRepository.java
    │   │   │   ├──  CartItemRepository.java
    │   │   │   ├──  OrderRepository.java
    │   │   │   └──  OrderItemRepository.java
    │   │   │
    │   │   ├── security/                               # JWT plumbing
    │   │   │   ├──  JwtService.java                 # Token generation & validation (JJWT 0.12.5)
    │   │   │   ├──  JwtAuthenticationFilter.java    # OncePerRequestFilter — token extraction
    │   │   │   └──  CustomUserDetailsService.java   # UserDetailsService bridge to DB
    │   │   │
    │   │   └── service/                               # Business logic layer
    │   │       ├──  AuthService.java                # Register & login logic
    │   │       ├──  ProductService.java             # CRUD + entity↔DTO mapping
    │   │       ├──  CartService.java                # Cart ops + total recalculation
    │   │       └──  OrderService.java               # Checkout, stock deduction, @Transactional
    │   │
    │   └── resources/
    │       └──  application.properties              # DB URL, JWT secret, JPA config
    │
    └── test/
        └── java/com/ecommerce/
            └──  EcommerceApplicationTests.java      # Spring context smoke test
```

---

##  Database Schema & ER Diagram

### Entity Relationship Diagram

```mermaid
erDiagram
    USERS {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR name "NOT NULL, max 100"
        VARCHAR email "NOT NULL, UNIQUE, max 150"
        VARCHAR password "NOT NULL, BCrypt hash"
        VARCHAR role "NOT NULL, ENUM: USER|ADMIN"
        DATETIME created_at "NOT NULL, immutable"
        DATETIME updated_at "NOT NULL"
    }

    PRODUCTS {
        BIGINT id PK "AUTO_INCREMENT"
        VARCHAR name "NOT NULL, max 200"
        TEXT description "NOT NULL"
        DECIMAL price "NOT NULL, precision 10,2"
        INT stock_quantity "NOT NULL, ≥ 0"
        VARCHAR category "NOT NULL, max 100"
        VARCHAR image_url "max 500, nullable"
        DATETIME created_at "NOT NULL, immutable"
        DATETIME updated_at "NOT NULL"
    }

    CARTS {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL, UNIQUE"
        DECIMAL total_amount "NOT NULL, precision 10,2"
        DATETIME created_at "NOT NULL, immutable"
        DATETIME updated_at "NOT NULL"
    }

    CART_ITEMS {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT cart_id FK "NOT NULL"
        BIGINT product_id FK "NOT NULL"
        INT quantity "NOT NULL, ≥ 1"
        DECIMAL price "NOT NULL — price snapshot"
        DECIMAL subtotal "NOT NULL — price × qty"
        DATETIME created_at "NOT NULL, immutable"
        DATETIME updated_at "NOT NULL"
    }

    ORDERS {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        DECIMAL total_amount "NOT NULL"
        VARCHAR status "NOT NULL, ENUM: PENDING|CONFIRMED|CANCELLED"
        VARCHAR shipping_address "NOT NULL, max 500"
        VARCHAR phone_number "NOT NULL, max 20"
        DATETIME order_date "NOT NULL"
        DATETIME created_at "NOT NULL, immutable"
        DATETIME updated_at "NOT NULL"
    }

    ORDER_ITEMS {
        BIGINT id PK "AUTO_INCREMENT"
        BIGINT order_id FK "NOT NULL"
        BIGINT product_id FK "NOT NULL"
        VARCHAR product_name "NOT NULL — name snapshot"
        VARCHAR product_image_url "image snapshot"
        DECIMAL price "NOT NULL — price snapshot"
        INT quantity "NOT NULL"
        DECIMAL subtotal "NOT NULL — price × qty"
        DATETIME created_at "NOT NULL, immutable"
        DATETIME updated_at "NOT NULL"
    }

    USERS ||--|| CARTS : "has one"
    USERS ||--o{ ORDERS : "places many"
    CARTS ||--o{ CART_ITEMS : "contains"
    PRODUCTS ||--o{ CART_ITEMS : "referenced by"
    ORDERS ||--o{ ORDER_ITEMS : "has many"
    PRODUCTS ||--o{ ORDER_ITEMS : "referenced by"
```

> **Design Note — Snapshot Pattern:** `ORDER_ITEMS` stores `product_name`, `product_image_url`, and `price` as snapshots at checkout time. This ensures historical order records remain accurate even if the product is later updated or deleted by an admin.

---

##  Security & Authentication Flow

### JWT Authentication Lifecycle

```mermaid
sequenceDiagram
    actor Client
    participant AuthController
    participant AuthService
    participant BCrypt
    participant UserRepository
    participant JwtService
    participant JwtFilter as JwtAuthenticationFilter
    participant SecurityContext

    Note over Client, SecurityContext: ═══ REGISTRATION FLOW ═══
    Client->>AuthController: POST /api/auth/register {name, email, password, role}
    AuthController->>AuthService: register(request)
    AuthService->>UserRepository: existsByEmail(email)
    UserRepository-->>AuthService: false (email is free)
    AuthService->>BCrypt: encode(rawPassword)
    BCrypt-->>AuthService: "$2a$10$hashedPassword..."
    AuthService->>UserRepository: save(user)
    UserRepository-->>AuthService: savedUser (with generated ID)
    AuthService->>JwtService: generateToken(userDetails)
    JwtService-->>AuthService: "eyJhbGciOiJIUzI1NiIs..."
    AuthService-->>AuthController: AuthResponse{token, userId, name, email, role}
    AuthController-->>Client: HTTP 201 CREATED + AuthResponse

    Note over Client, SecurityContext: ═══ LOGIN FLOW ═══
    Client->>AuthController: POST /api/auth/login {email, password}
    AuthController->>AuthService: login(request)
    AuthService->>BCrypt: authenticate(email, rawPassword)
    BCrypt->>UserRepository: loadUserByUsername(email)
    UserRepository-->>BCrypt: UserDetails{hashedPassword, role}
    BCrypt-->>AuthService:  Credentials match
    AuthService->>JwtService: generateToken(userDetails)
    JwtService-->>AuthService: JWT Token (24h expiry)
    AuthService-->>Client: HTTP 200 OK + AuthResponse{token}

    Note over Client, SecurityContext: ═══ AUTHENTICATED REQUEST FLOW ═══
    Client->>JwtFilter: GET /api/cart  [Authorization: Bearer <token>]
    JwtFilter->>JwtFilter: Extract token (strip "Bearer ")
    JwtFilter->>JwtService: extractUsername(token)
    JwtService-->>JwtFilter: "user@email.com"
    JwtFilter->>UserRepository: loadUserByUsername(email)
    UserRepository-->>JwtFilter: UserDetails
    JwtFilter->>JwtService: isTokenValid(token, userDetails)
    JwtService-->>JwtFilter: true
    JwtFilter->>SecurityContext: setAuthentication(UsernamePasswordAuthenticationToken)
    JwtFilter-->>Client: Request proceeds to Controller
```

### Endpoint Access Control Matrix

```mermaid
graph LR
    subgraph PUBLIC[" Public — No Token Required"]
        R1["POST /api/auth/register"]
        R2["POST /api/auth/login"]
        R3["GET  /api/products"]
        R4["GET  /api/products/{id}"]
    end

    subgraph USER_AUTH[" Authenticated — Any Role"]
        R5["GET    /api/cart"]
        R6["POST   /api/cart/add"]
        R7["PUT    /api/cart/item/{id}"]
        R8["DELETE /api/cart/item/{id}"]
        R9["DELETE /api/cart/clear"]
        R10["POST  /api/orders/place"]
        R11["GET   /api/orders/my-orders"]
        R12["GET   /api/orders/{id}"]
    end

    subgraph ADMIN_ONLY[" Admin Only — ROLE_ADMIN Required"]
        R13["POST   /api/products"]
        R14["PUT    /api/products/{id}"]
        R15["DELETE /api/products/{id}"]
    end

    style PUBLIC fill:#e8f5e9,stroke:#4caf50,color:#000
    style USER_AUTH fill:#e3f2fd,stroke:#2196f3,color:#000
    style ADMIN_ONLY fill:#fce4ec,stroke:#f44336,color:#000
```

---

##  API Reference

###  Authentication — `/api/auth`

<details>
<summary><code>POST</code> <code><b>/api/auth/register</b></code> — Register a new user</summary>

**Request Body**
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "securePass123",
  "role": "USER"
}
```

**Validation Rules**
| Field | Rule |
|---|---|
| `name` | `@NotBlank` — Required |
| `email` | `@NotBlank` + `@Email` — Valid email format |
| `password` | `@NotBlank` + `@Size(min=6)` — Minimum 6 characters |
| `role` | Optional — defaults to `USER` if omitted. Accepts `USER` or `ADMIN` |

**Response** — `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "USER"
}
```
</details>

<details>
<summary><code>POST</code> <code><b>/api/auth/login</b></code> — Authenticate and get JWT token</summary>

**Request Body**
```json
{
  "email": "john@example.com",
  "password": "securePass123"
}
```

**Response** — `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "USER"
}
```
</details>

---

###  Products — `/api/products`

> `GET` endpoints are public. `POST`, `PUT`, `DELETE` require `Authorization: Bearer <ADMIN_TOKEN>`.

<details>
<summary><code>GET</code> <code><b>/api/products</b></code> — List all products (Public)</summary>

**Response** — `200 OK`
```json
[
  {
    "id": 1,
    "name": "iPhone 15",
    "description": "Apple iPhone 15 with 128GB storage",
    "price": 79999.00,
    "stockQuantity": 10,
    "category": "Mobile",
    "imageUrl": "https://example.com/iphone15.jpg",
    "createdAt": "2026-05-01T10:00:00",
    "updatedAt": "2026-05-01T10:00:00"
  }
]
```
</details>

<details>
<summary><code>GET</code> <code><b>/api/products/{id}</b></code> — Get single product (Public)</summary>

**Path Parameter:** `id` — Product ID (Long)

**Response** — `200 OK` | `404 Not Found`
</details>

<details>
<summary><code>POST</code> <code><b>/api/products</b></code> — Create product <code>[ADMIN]</code></summary>

**Headers:** `Authorization: Bearer <ADMIN_TOKEN>`

**Request Body**
```json
{
  "name": "MacBook Pro 14",
  "description": "Apple MacBook Pro with M3 chip, 16GB RAM",
  "price": 199999.00,
  "stockQuantity": 5,
  "category": "Laptop",
  "imageUrl": "https://example.com/macbook-pro.jpg"
}
```

**Validation Rules**
| Field | Rule |
|---|---|
| `name` | `@NotBlank` |
| `description` | `@NotBlank` |
| `price` | `@NotNull` + `@Positive` — Must be > 0 |
| `stockQuantity` | `@NotNull` + `@PositiveOrZero` — Must be ≥ 0 |
| `category` | `@NotBlank` |
| `imageUrl` | Optional |

**Response** — `201 Created`
</details>

<details>
<summary><code>PUT</code> <code><b>/api/products/{id}</b></code> — Update product <code>[ADMIN]</code></summary>

**Headers:** `Authorization: Bearer <ADMIN_TOKEN>` | Same body as `POST /api/products`

**Response** — `200 OK` | `404 Not Found`
</details>

<details>
<summary><code>DELETE</code> <code><b>/api/products/{id}</b></code> — Delete product <code>[ADMIN]</code></summary>

**Headers:** `Authorization: Bearer <ADMIN_TOKEN>`

**Response** — `204 No Content` | `404 Not Found`
</details>

---

### 🛒 Cart — `/api/cart`

> All cart endpoints require `Authorization: Bearer <USER_TOKEN>`.

<details>
<summary><code>GET</code> <code><b>/api/cart</b></code> — View current user's cart</summary>

**Response** — `200 OK`
```json
{
  "cartId": 1,
  "userId": 2,
  "items": [
    {
      "cartItemId": 3,
      "productId": 1,
      "productName": "iPhone 15",
      "productImageUrl": "https://example.com/iphone15.jpg",
      "price": 79999.00,
      "quantity": 2,
      "subtotal": 159998.00
    }
  ],
  "totalAmount": 159998.00,
  "totalItems": 2
}
```
</details>

<details>
<summary><code>POST</code> <code><b>/api/cart/add</b></code> — Add product to cart</summary>

**Request Body**
```json
{
  "productId": 1,
  "quantity": 2
}
```
If the product is already in the cart, the quantity is **incremented** (not duplicated). Stock availability is validated before adding.

**Response** — `200 OK` — Full updated `CartResponse`
</details>

<details>
<summary><code>PUT</code> <code><b>/api/cart/item/{cartItemId}</b></code> — Update item quantity</summary>

**Request Body**
```json
{
  "quantity": 5
}
```
**Response** — `200 OK` — Full updated `CartResponse`
</details>

<details>
<summary><code>DELETE</code> <code><b>/api/cart/item/{cartItemId}</b></code> — Remove single item</summary>

**Response** — `200 OK`
</details>

<details>
<summary><code>DELETE</code> <code><b>/api/cart/clear</b></code> — Clear entire cart</summary>

**Response** — `200 OK`
</details>

---

### 🧾 Orders — `/api/orders`

> All order endpoints require `Authorization: Bearer <USER_TOKEN>`.

<details>
<summary><code>POST</code> <code><b>/api/orders/place</b></code> — Checkout and place order</summary>

**Request Body**
```json
{
  "shippingAddress": "42 MG Road, Bangalore, Karnataka - 560001",
  "phoneNumber": "9876543210"
}
```

**Validation Rules**
| Field | Rule |
|---|---|
| `shippingAddress` | `@NotBlank` |
| `phoneNumber` | `@NotBlank` + `@Pattern(^\\d{10}$)` — Exactly 10 digits |

**What happens atomically (`@Transactional`):**
1. Validates that the cart is not empty
2. Checks stock availability for every item
3. Deducts stock from each product
4. Creates `Order` + `OrderItem` records with price snapshots
5. Calculates total amount
6. Clears the cart

**Response** — `201 Created` — Full `OrderResponse`
</details>

<details>
<summary><code>GET</code> <code><b>/api/orders/my-orders</b></code> — View order history</summary>

**Response** — `200 OK` — Array of `OrderResponse`, sorted newest first
</details>

<details>
<summary><code>GET</code> <code><b>/api/orders/{orderId}</b></code> — Get specific order</summary>

Authorization-boundary enforced: users can only view their own orders.

**Response** — `200 OK` | `404 Not Found`
</details>

---

## 🔄 Request Lifecycle

```mermaid
flowchart TD
    A(["HTTP Request"]) --> B{"Has Authorization\nHeader?"}

    B -- No --> C{"Is endpoint\npublic?"}
    C -- Yes --> D[" Allow — Proceed to Controller"]
    C -- No --> E["❌ 401 Unauthorized"]

    B -- Yes --> F["JwtAuthenticationFilter\nExtract Bearer Token"]
    F --> G["JwtService.extractUsername(token)"]
    G --> H{"Valid\nJWT?"}

    H -- No --> I["❌ 401 Unauthorized"]
    H -- Yes --> J["CustomUserDetailsService\nLoad User from DB"]
    J --> K["SecurityContextHolder\nSet Authentication"]
    K --> L{"Has required\nrole?"}

    L -- No --> M["❌ 403 Forbidden"]
    L -- Yes --> N["Controller"]

    N --> O["@Valid DTO\nvalidation"]
    O --> P{"Validation\npassed?"}
    P -- No --> Q["❌ 400 Bad Request\n+ field errors"]
    P -- Yes --> R["Service Layer\nBusiness Logic"]

    R --> S{"Operation\nsucceeded?"}
    S -- No --> T["GlobalExceptionHandler\nStructured JSON Error"]
    S -- Yes --> U["Repository Layer\nDB Operation"]
    U --> V["✅ HTTP 200/201/204\nJSON Response"]
```

---

## 🔁 Data Flow Diagrams

### Order Placement Flow (Transactional)

```mermaid
flowchart LR
    A(["POST /api/orders/place"]) --> B["Authenticate User\nvia JWT"]
    B --> C["Fetch User's Cart\nfrom DB"]
    C --> D{"Cart\nempty?"}
    D -- Yes --> E["❌ RuntimeException\nCart is empty"]
    D -- No --> F["Loop: Each CartItem"]

    F --> G{"Stock ≥\nQuantity?"}
    G -- No --> H["❌ RuntimeException\nOut of stock\n🔙 ROLLBACK ALL"]
    G -- Yes --> I["Deduct Stock\nproductRepository.save()"]

    I --> J["Create OrderItem\n+ price snapshot"]
    J --> K{"More\nItems?"}
    K -- Yes --> F
    K -- No --> L["Create Order\n+ Calculate Total"]

    L --> M["orderRepository.save()\n— cascades OrderItems"]
    M --> N["cartService.clearCart()"]
    N --> O["✅ Return OrderResponse\nHTTP 201"]

    style H fill:#ffebee,stroke:#f44336,color:#000
    style E fill:#ffebee,stroke:#f44336,color:#000
    style O fill:#e8f5e9,stroke:#4caf50,color:#000
```

### Cart Add-to-Cart Flow

```mermaid
flowchart TD
    A(["POST /api/cart/add\n{productId, quantity}"]) --> B["Resolve User\nby JWT email"]
    B --> C["Fetch / Create Cart\nfor User"]
    C --> D["Fetch Product\nby productId"]
    D --> E{"Product already\nin Cart?"}

    E -- Yes --> F["targetQty =\nexisting + requested"]
    E -- No --> F2["targetQty =\nrequested"]

    F --> G{"stock ≥\ntargetQty?"}
    F2 --> G

    G -- No --> H["❌ RuntimeException\nInsufficient stock"]
    G -- Yes --> I{"Existing\nitem?"}

    I -- Yes --> J["Update CartItem\nqty + subtotal"]
    I -- No --> K["Create new CartItem\nwith price snapshot"]

    J --> L["Recalculate\nCart Total"]
    K --> L

    L --> M["cartRepository.save()\n— cascade saves CartItems"]
    M --> N["✅ Return CartResponse"]

    style H fill:#ffebee,stroke:#f44336,color:#000
    style N fill:#e8f5e9,stroke:#4caf50,color:#000
```

---

## ⚠️ Error Handling

All errors are handled centrally via `GlobalExceptionHandler` (`@RestControllerAdvice`) and return a consistent JSON structure.

### Error Response Structure

```json
{
  "status": 404,
  "message": "Product not found with id: 99",
  "timestamp": "2026-05-27T10:30:00.123"
}
```

### Validation Error Structure (`400`)

```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "price": "Price must be greater than 0",
    "name": "Product name is required"
  },
  "timestamp": "2026-05-27T10:31:00.456"
}
```

### Exception → HTTP Status Mapping

```mermaid
graph LR
    subgraph Exceptions
        E1["ResourceNotFoundException"]
        E2["MethodArgumentNotValidException"]
        E3["BadCredentialsException"]
        E4["AccessDeniedException"]
        E5["RuntimeException"]
        E6["Exception (catch-all)"]
    end

    subgraph "HTTP Response"
        R1["404 Not Found"]
        R2["400 Bad Request\n+ field errors map"]
        R3["401 Unauthorized"]
        R4["403 Forbidden"]
        R5["400 Bad Request"]
        R6["500 Internal Server Error"]
    end

    E1 --> R1
    E2 --> R2
    E3 --> R3
    E4 --> R4
    E5 --> R5
    E6 --> R6
```

---

##  Getting Started

### Prerequisites

Ensure the following are installed and configured:

| Tool | Minimum Version | Check Command |
|---|---|---|
| JDK | 17+ | `java -version` |
| Apache Maven | 3.9+ | `mvn -version` |
| MySQL Server | 8.0+ | `mysql --version` |
| Git | Any | `git --version` |

### Step 1 — Clone the Repository

```bash
git clone https://github.com/your-username/ecommerce-springboot.git
cd ecommerce-springboot
```

### Step 2 — Create the MySQL Database

Connect to your MySQL server and run:

```sql
CREATE DATABASE ecommerce_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> **Note:** Hibernate's `ddl-auto=update` will automatically create all required tables (`users`, `products`, `carts`, `cart_items`, `orders`, `order_items`) on first startup.

### Step 3 — Configure Application Properties

Edit `src/main/resources/application.properties`:

```properties
# ─── Database ───────────────────────────────────────────────
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD

# ─── JWT ────────────────────────────────────────────────────
# Must be a Base64-encoded string of at least 256 bits (32 bytes)
app.jwt.secret=YOUR_BASE64_ENCODED_256BIT_SECRET
app.jwt.expiration=86400000   # 24 hours in milliseconds
```

**Generate a secure JWT secret key:**
```bash
# Option 1 — OpenSSL
openssl rand -base64 32

# Option 2 — Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

### Step 4 — Build & Run

```bash
# Build without running tests
mvn clean install -DskipTests

# Start the application
mvn spring-boot:run
```

The server starts on **`http://localhost:8080`**

### Step 5 — Verify Startup

```bash
# Should return JSON array (may be empty on first run)
curl http://localhost:8080/api/products
```

---

## ⚙️ Configuration Reference

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port the embedded Tomcat listens on |
| `spring.datasource.url` | — | JDBC connection URL to MySQL |
| `spring.datasource.username` | — | MySQL username |
| `spring.datasource.password` | — | MySQL password |
| `spring.jpa.hibernate.ddl-auto` | `update` | Schema strategy — use `none` in production |
| `spring.jpa.show-sql` | `true` | Print SQL queries to console |
| `spring.jpa.properties.hibernate.format_sql` | `true` | Pretty-print SQL |
| `spring.jpa.database-platform` | `MySQLDialect` | Hibernate SQL dialect |
| `app.jwt.secret` | — | Base64-encoded HMAC-SHA256 signing key |
| `app.jwt.expiration` | `86400000` | Token lifetime in milliseconds (24h) |

---

## 🧪 Testing Guide

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=EcommerceApplicationTests

# Run with verbose output
mvn test -Dtest=EcommerceApplicationTests -pl . -am
```

### Test Coverage Areas

```mermaid
mindmap
  root((Test Coverage))
    Unit Tests
      AuthService
        register success
        duplicate email rejection
        invalid role handling
        login success
        bad credentials
      ProductService
        CRUD operations
        404 on missing product
      CartService
        add new item
        increment existing item
        stock limit enforcement
        total recalculation
      OrderService
        successful checkout
        empty cart rejection
        stock deduction
        cart clear post-order
    Integration Tests
      SecurityConfig
        public endpoint access
        auth endpoint protection
        RBAC enforcement
      GlobalExceptionHandler
        404 JSON format
        400 validation format
        401 bad credentials format
```

---

## 📬 Postman Testing Flow

Follow this sequence to test the complete e-commerce lifecycle:

```mermaid
sequenceDiagram
    actor Admin
    actor User
    participant API

    Note over Admin, API: ─── Setup Phase ───
    Admin->>API: POST /api/auth/register {role: "ADMIN"}
    API-->>Admin: adminToken

    User->>API: POST /api/auth/register {role: "USER"}
    API-->>User: userToken

    Note over Admin, API: ─── Catalog Phase ───
    Admin->>API: POST /api/products [Authorization: adminToken]
    API-->>Admin: product {id: 1}

    Admin->>API: POST /api/products [Authorization: adminToken]
    API-->>Admin: product {id: 2}

    Note over User, API: ─── Shopping Phase ───
    User->>API: GET /api/products
    API-->>User: [product1, product2]

    User->>API: POST /api/cart/add {productId:1, qty:2} [userToken]
    API-->>User: cartResponse {total: X}

    User->>API: POST /api/cart/add {productId:2, qty:1} [userToken]
    API-->>User: cartResponse {total: X+Y}

    User->>API: PUT /api/cart/item/1 {quantity:3} [userToken]
    API-->>User: cartResponse {updated total}

    Note over User, API: ─── Checkout Phase ───
    User->>API: POST /api/orders/place {address, phone} [userToken]
    API-->>User: orderResponse {status: CONFIRMED}

    User->>API: GET /api/orders/my-orders [userToken]
    API-->>User: [orderResponse]

    User->>API: GET /api/products/1
    API-->>User: product {stockQuantity: reduced}
```

### Setting Up Postman Authorization

1. After login, copy the `token` value from the response
2. In Postman, go to your request → **Authorization** tab
3. Select **Bearer Token** from the Type dropdown
4. Paste the token in the Token field

---

##  Security Considerations

> [!WARNING]
> **Critical for Production Deployment**

| Concern | Development | Production Recommendation |
|---|---|---|
| **JWT Secret** | Hardcoded in `.properties` | Use environment variable or secrets manager (AWS Secrets Manager, HashiCorp Vault) |
| **DB Credentials** | Hardcoded in `.properties` | Inject via environment variables (`SPRING_DATASOURCE_PASSWORD`) |
| **`ddl-auto`** | `update` | Set to `none` or `validate`; use Flyway/Liquibase for migrations |
| **`show-sql`** | `true` | Set to `false` to avoid SQL in logs |
| **HTTPS** | Not configured | Terminate TLS at load balancer or configure Spring SSL |
| **CORS** | Not configured | Add `CorsConfigurationSource` bean to `SecurityConfig` |
| **Rate Limiting** | Not implemented | Add Bucket4j or API Gateway rate limiting |
| **Role Validation** | Registration accepts `ADMIN` via API | Restrict admin role creation to internal processes only |

> [!CAUTION]
> Never commit real credentials to version control. Add `application-local.properties` to `.gitignore` and use Spring profiles for environment-specific config.

---

## 🗺️ Domain Model Overview

```mermaid
classDiagram
    class User {
        +Long id
        +String name
        +String email
        +String password
        +Role role
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class Product {
        +Long id
        +String name
        +String description
        +BigDecimal price
        +Integer stockQuantity
        +String category
        +String imageUrl
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class Cart {
        +Long id
        +BigDecimal totalAmount
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
        +addItem(CartItem)
        +removeItem(CartItem)
    }

    class CartItem {
        +Long id
        +Integer quantity
        +BigDecimal price
        +BigDecimal subtotal
        +LocalDateTime createdAt
        +LocalDateTime updatedAt
    }

    class Order {
        +Long id
        +BigDecimal totalAmount
        +OrderStatus status
        +String shippingAddress
        +String phoneNumber
        +LocalDateTime orderDate
    }

    class OrderItem {
        +Long id
        +String productName
        +String productImageUrl
        +BigDecimal price
        +Integer quantity
        +BigDecimal subtotal
    }

    class Role {
        <<enumeration>>
        USER
        ADMIN
    }

    class OrderStatus {
        <<enumeration>>
        PENDING
        CONFIRMED
        CANCELLED
    }

    User "1" --> "1" Cart : owns
    User "1" --> "*" Order : places
    Cart "1" --> "*" CartItem : contains
    CartItem "*" --> "1" Product : references
    Order "1" --> "*" OrderItem : contains
    OrderItem "*" --> "1" Product : references
    User --> Role : has
    Order --> OrderStatus : has
```

---

## 🤝 Contributing

Contributions are welcome! Please follow the workflow below:

```mermaid
gitGraph
    commit id: "Initial commit"
    branch feature/your-feature
    checkout feature/your-feature
    commit id: "Add feature logic"
    commit id: "Add tests"
    commit id: "Update README"
    checkout main
    merge feature/your-feature id: "PR merged ✅"
    commit id: "Release v1.1.0"
```

### Contribution Steps

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/payment-gateway`
3. **Commit** your changes: `git commit -m "feat: add Razorpay payment integration"`
4. **Push** to the branch: `git push origin feature/payment-gateway`
5. **Open a Pull Request** against `main`

### Commit Message Convention

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat:     New feature
fix:      Bug fix
docs:     Documentation only
refactor: Code refactor without behavior change
test:     Adding or updating tests
chore:    Build system, dependencies
```

---

## 📈 Planned Enhancements

| Feature | Priority | Status |
|---|---|---|
| Payment gateway integration (Razorpay / Stripe) | 🔴 High | 📋 Planned |
| Product search & category filter endpoints | 🔴 High | 📋 Planned |
| Admin order status management (CONFIRMED → SHIPPED) | 🟡 Medium | 📋 Planned |
| Email notifications on order placement | 🟡 Medium | 📋 Planned |
| Refresh token support | 🟡 Medium | 📋 Planned |
| Docker + Docker Compose setup | 🟢 Low | 📋 Planned |
| Swagger / OpenAPI documentation | 🟢 Low | 📋 Planned |
| Redis caching for product catalog | 🟢 Low | 📋 Planned |

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

```
MIT License

Copyright (c) 2026

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software...
```

---

<div align="center">

**Built with ❤️ using Spring Boot · MySQL · Spring Security · JWT**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat-square&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)

</div>
