package pro.sinatra.mini.test;

import org.flywaydb.core.Flyway;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Global TestExecutionListener that runs before any tests.
 * It starts Testcontainers Postgres (when available) or falls
 * back to an in-memory H2 database, applies Flyway migrations
 * and sets system properties so Spring Test picks the same
 * datasource used by Flyway.
 */
public class GlobalTestBootstrap implements TestExecutionListener {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static PostgreSQLContainer<?> POSTGRES_CONTAINER;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        // If running tests against an external Postgres (e.g. k8s), skip Testcontainers.
        String useK8s = System.getProperty("tests.use.k8s", System.getenv("TESTS_USE_K8S"));
        if ("true".equalsIgnoreCase(useK8s)) {
            System.err.println("[GlobalTestBootstrap] Skipping Testcontainers because TESTS_USE_K8S=true");
            return;
        }
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        try {
            POSTGRES_CONTAINER = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                    .withDatabaseName("sinatra_test")
                    .withUsername("test")
                    .withPassword("test");
            POSTGRES_CONTAINER.start();

            String url = POSTGRES_CONTAINER.getJdbcUrl();

            Flyway flyway = Flyway.configure()
                    .dataSource(url, POSTGRES_CONTAINER.getUsername(), POSTGRES_CONTAINER.getPassword())
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();

            // Prevent Spring Boot from running Flyway again
            System.setProperty("spring.flyway.enabled", "false");

            // Export datasource as system properties for Spring Test
            System.setProperty("spring.datasource.url", url);
            System.setProperty("spring.datasource.username", POSTGRES_CONTAINER.getUsername());
            System.setProperty("spring.datasource.password", POSTGRES_CONTAINER.getPassword());

            System.err.println("[GlobalTestBootstrap] Testcontainers Postgres started and migrations applied.");
        } catch (Exception ex) {
            System.err.println("[GlobalTestBootstrap] Testcontainers not available: " + ex.getMessage());

            // Fallback to H2 in-memory
            String h2Url = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
            System.setProperty("spring.datasource.url", h2Url);
            System.setProperty("spring.datasource.username", "sa");
            System.setProperty("spring.datasource.password", "");
            System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");

            try {
                Flyway flywayFallback = Flyway.configure()
                        .dataSource(h2Url, "sa", "")
                        .locations("classpath:db/migration-test")
                        .baselineOnMigrate(true)
                        .load();
                flywayFallback.migrate();
                System.setProperty("spring.flyway.enabled", "false");
                System.err.println("[GlobalTestBootstrap] Flyway fallback to H2 applied test migrations.");
            } catch (Exception ex2) {
                System.err.println("[GlobalTestBootstrap] Flyway fallback to H2 failed: " + ex2.getMessage());
            }
        }
    }

}
