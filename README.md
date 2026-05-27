# Spring Boot E-Commerce Backend

A robust, beginner-friendly e-commerce backend built with **Spring Boot** and **Java 17**. This project implements core e-commerce backend capabilities, including user authentication, product catalog CRUD, shopping cart management, and order processing, backed by a MySQL database.

---

## 🌟 Features

*   **User Registration & Login:** Dedicated endpoints for registering users/admins and logging in.
*   **JWT Security:** Secured REST APIs using JSON Web Token (JWT) stateless authentication.
*   **Role-Based Access Control (RBAC):** Restricts certain endpoints (such as product creation, updates, and deletion) to users with the `ADMIN` role, while allowing customers (`USER`) to manage their carts and place orders.
*   **Product Catalog Management:** Complete CRUD (Create, Read, Update, Delete) capability for shop products.
*   **Shopping Cart Management:** Add items, view the current cart, update quantities, remove items, and clear the cart, featuring automatic database calculations for totals and item stock checks.
*   **Order Processing:** Checkout cart items, place orders with address verification, automatically reduce product stock, clear the shopping cart upon success, and track order histories.
*   **MySQL Database Integration:** Relational database storage with clean entity mappings (One-to-One, One-to-Many, Many-to-One) and cascading rules.

---

## 🛠️ Tech Stack

*   **Language:** Java 17
*   **Framework:** Spring Boot 3.2.5 (Spring Web, Spring Security, Spring Data JPA)
*   **Database:** MySQL
*   **Build Tool:** Maven
*   **Libraries:** Lombok, JSON Web Tokens (jjwt)
*   **Testing & Operations:** Postman, PowerShell, Git/GitHub

---

## 📂 Project Folder Structure

```text
ecommerce-springboot/
│
├── src/
│   ├── main/
│   │   ├── java/com/ecommerce/
│   │   │   ├── config/             # Security configurations (CORS, Endpoint rules)
│   │   │   ├── controller/         # REST API Controllers (endpoints exposing endpoints)
│   │   │   ├── dto/                # Request & Response Data Transfer Objects (DTOs)
│   │   │   ├── entity/             # JPA Entities mapped to MySQL database tables
│   │   │   ├── exception/          # Global Exception Handler and custom exception classes
│   │   │   ├── repository/         # Spring Data JPA Repository interfaces
│   │   │   ├── security/           # JWT filtering, extraction, and validation logic
│   │   │   ├── service/            # Core business logic implementation
│   │   │   └── EcommerceApplication.java
│   │   │
│   │   └── resources/
│   │       └── application.properties # Main application configuration settings
│   │
│   └── test/                       # Unit tests
│
├── .gitignore                      # Specified ignored files and folders in Git
├── pom.xml                         # Maven dependencies & build properties
└── README.md                       # Project documentation
```

---

## 🚀 API Endpoints

### 🔐 Authentication (`/api/auth`)
*   `POST /api/auth/register` — Registers a new user or admin.
*   `POST /api/auth/login` — Authenticates user credentials and returns a JWT token.

### 📦 Products (`/api/products`)
*   `GET /api/products` — Retrieve a list of all products *(Public)*.
*   `GET /api/products/{id}` — Retrieve a single product by ID *(Public)*.
*   `POST /api/products` — Create a new product *(Admin Only)*.
*   `PUT /api/products/{id}` — Update an existing product *(Admin Only)*.
*   `DELETE /api/products/{id}` — Delete a product *(Admin Only)*.

### 🛒 Shopping Cart (`/api/cart`)
*   `GET /api/cart` — View the logged-in user's cart.
*   `POST /api/cart/add` — Add a product to the cart (or increment quantity).
*   `PUT /api/cart/item/{cartItemId}` — Update item quantity in the cart.
*   `DELETE /api/cart/item/{cartItemId}` — Remove a specific item from the cart.
*   `DELETE /api/cart/clear` — Clear the entire cart.

### 🧾 Orders (`/api/orders`)
*   `POST /api/orders/place` — Checkout the cart and place an order.
*   `GET /api/orders/my-orders` — View the logged-in user's order history.
*   `GET /api/orders/{id}` — Retrieve details of a specific order (Owner or Admin only).

---

## 💻 How to Run Locally

### 1. Prerequisites
*   Ensure **Java 17** (or above) and **Maven** are installed.
*   Ensure **MySQL** is running. Create a schema called `ecommerce_db`:
    ```sql
    CREATE DATABASE ecommerce_db;
    ```

### 2. Configure Credentials
Open the `src/main/resources/application.properties` file and customize the database configuration to match your local setup:
```properties
spring.datasource.username=YOUR_MYSQL_USERNAME
spring.datasource.password=YOUR_MYSQL_PASSWORD
app.jwt.secret=YOUR_JWT_SECRET_KEY
```

### 3. Build & Run
Open your terminal and run the following commands:
```bash
cd C:\Users\visit\Downloads\ecommerce-springboot
mvn clean install -DskipTests
mvn spring-boot:run
```

The server should start on port `8080`.

### 4. Verify in Browser
Open your browser and navigate to:
[http://localhost:8080/api/products](http://localhost:8080/api/products)

---

## 🧪 Postman Testing Flow

To test the APIs in Postman, configure the request headers to send authorization tokens as `Bearer <token>` for all secured endpoints.

1.  **Register Admin:**
    *   `POST /api/auth/register` with role `ADMIN`.
2.  **Register User:**
    *   `POST /api/auth/register` with role `USER`.
3.  **Login & Obtain JWT Token:**
    *   `POST /api/auth/login` using registered credentials. Copy the `token` value.
4.  **Create Product (as Admin):**
    *   `POST /api/products` using the **Admin's** token in the `Authorization` header.
5.  **Add to Cart (as User):**
    *   `POST /api/cart/add` using the **User's** token in the `Authorization` header.
6.  **Place Order (as User):**
    *   `POST /api/orders/place` with shipping address and phone number to checkout the cart.
7.  **View Orders (as User):**
    *   `GET /api/orders/my-orders` to view order details and check if the stock was reduced.

---

## 🔒 GitHub Security Note

> [!WARNING]
> **Keep Credentials Safe!**
> Never upload your real database credentials or signing keys directly to a public GitHub repository. Always replace database passwords and signing secrets in `application.properties` with placeholders (e.g. `YOUR_MYSQL_PASSWORD`) before checking in code, and utilize environment variables in production environments.
