package com.scribble.testconfig;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 * Uses the test profile which connects to the Docker Compose services
 * (PostgreSQL on port 5433, Redis on port 6380).
 *
 * Testcontainers support is included in dependencies for environments
 * where Docker API is compatible (API >= 1.40).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @LocalServerPort
    protected int port;
}
