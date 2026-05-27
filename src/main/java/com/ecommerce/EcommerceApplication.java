package com.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

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
 *   5. Activates Redis caching for faster product lookups.
 *
 * @SpringBootApplication is a shortcut that combines 3 annotations:
 *   - @Configuration   → This class can define settings (beans).
 *   - @EnableAutoConfiguration → Spring Boot auto-configures things
 *                                 based on the dependencies in pom.xml.
 *   - @ComponentScan   → Scans all sub-packages for code to load.
 *
 * @EnableCaching → Turns ON the caching system.
 *   Without this, @Cacheable and @CacheEvict annotations in
 *   our service classes will be completely ignored by Spring.
 *   Think of it as the "master switch" for caching.
 */
@SpringBootApplication
@EnableCaching
public class EcommerceApplication {

    public static void main(String[] args) {
        // This single line launches the entire Spring Boot application.
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
