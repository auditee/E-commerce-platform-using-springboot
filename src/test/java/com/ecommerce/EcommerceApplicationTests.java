package com.ecommerce;

import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;

/**
 * ============================================================
 * EcommerceApplicationTests.java — Basic test class.
 * ============================================================
 *
 * This test simply checks if the Spring Boot application
 * can start up without errors. If the database connection
 * fails or if there's a configuration mistake, this test
 * will fail and tell us something is wrong.
 *
 * @SpringBootTest → Loads the full application for testing.
 */
@SpringBootTest
class EcommerceApplicationTests {

    @Test
    void contextLoads() {
        // This test passes if the application starts without errors.
        // No code needed here — just loading the context is the test.
    }
}
