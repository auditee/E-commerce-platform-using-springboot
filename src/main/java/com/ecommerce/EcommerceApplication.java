package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ============================================================
 * EcommerceApplication.java — The starting point of our app.
 * ============================================================
 *
 * Think of this file as the "power button" for the entire backend.
 * When you run this file, Spring Boot:
 *   1. Starts a built-in web server (Tomcat) on port 8080.
 *   2. Scans all packages for our code (controllers, services, etc.).
 *   3. Connects to the MySQL database using our settings.
 *   4. Makes our REST APIs available at http://localhost:8080.
 *
 * @SpringBootApplication is a shortcut that combines 3 annotations:
 *   - @Configuration   → This class can define settings (beans).
 *   - @EnableAutoConfiguration → Spring Boot auto-configures things
 *                                 based on the dependencies in pom.xml.
 *   - @ComponentScan   → Scans all sub-packages for code to load.
 */
@SpringBootApplication
public class EcommerceApplication {

    public static void main(String[] args) {
        // This single line launches the entire Spring Boot application.
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
