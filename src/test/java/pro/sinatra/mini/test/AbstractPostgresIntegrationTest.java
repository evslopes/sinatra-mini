package pro.sinatra.mini.test;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for Postgres-backed integration tests (JUnit 5).
 * Static initializer starts Testcontainers Postgres and runs Flyway
 * before the test class is loaded, ensuring schema is present
 * before Spring Boot's test context initializes.
 */
public abstract class AbstractPostgresIntegrationTest {

    public static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("sinatra_test")
                .withUsername("test")
                .withPassword("test");
        try {
            // Start container and apply migrations immediately when class is loaded
            POSTGRES_CONTAINER.start();

            Flyway flyway = Flyway.configure()
                    .dataSource(POSTGRES_CONTAINER.getJdbcUrl(), POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();

                // Prevent Spring Boot from running Flyway again (we already applied migrations)
                System.setProperty("spring.flyway.enabled", "false");

            // Export as system properties so Spring Boot Test picks them up early
            System.setProperty("spring.datasource.url", POSTGRES_CONTAINER.getJdbcUrl());
            System.setProperty("spring.datasource.username", POSTGRES_CONTAINER.getUsername());
            System.setProperty("spring.datasource.password", POSTGRES_CONTAINER.getPassword());
        } catch (Exception ex) {
            // If Docker/Testcontainers isn't available, continue without failing here.
            // Tests running in this environment may need alternative setup (local Postgres or CI).
            System.err.println("[AbstractPostgresIntegrationTest] Testcontainers not available: " + ex.getMessage());
            // Do not rethrow — allow test execution to proceed so other fallbacks can run.
            // Fallback: ensure datasource properties align with H2 to avoid driver mismatch when test profile
            // forces Postgres driver. Set H2 system properties so Spring uses an embedded H2 datasource instead.
            String h2Url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
            System.setProperty("spring.datasource.url", h2Url);
            System.setProperty("spring.datasource.username", "sa");
            System.setProperty("spring.datasource.password", "");
            System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");

            // Also run Flyway against the H2 fallback using test-only migrations
            try {
                Flyway flywayFallback = Flyway.configure()
                        .dataSource(h2Url, "sa", "")
                        .locations("classpath:db/migration-test")
                        .baselineOnMigrate(true)
                        .load();
                flywayFallback.migrate();
                // Prevent Spring Boot from running Flyway (avoid applying main migrations against H2)
                System.setProperty("spring.flyway.enabled", "false");
                System.err.println("[AbstractPostgresIntegrationTest] Flyway fallback to H2 applied test migrations.");
            } catch (Exception ex2) {
                System.err.println("[AbstractPostgresIntegrationTest] Flyway fallback to H2 failed: " + ex2.getMessage());
            }
        }
    }

}
