package pro.sinatra.mini.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class LocalDockerDiagnosticTest {

    @Test
    public void tryStartPostgresContainer_andPrintStackOnError() {
        String useK8s = System.getProperty("tests.use.k8s", System.getenv("TESTS_USE_K8S"));
        Assumptions.assumeFalse("true".equalsIgnoreCase(useK8s), "Skipping local Docker diagnostic when TESTS_USE_K8S=true");

        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("sinatra_test")
                .withUsername("test")
                .withPassword("test");
        try {
            container.start();
            System.err.println("[LocalDockerDiagnosticTest] Container started: " + container.getContainerId());
            container.stop();
        } catch (Throwable t) {
            System.err.println("[LocalDockerDiagnosticTest] Failed to start Testcontainers Postgres:");
            t.printStackTrace(System.err);
            // rethrow so test fails and CI shows details
            throw t;
        }
    }

}
