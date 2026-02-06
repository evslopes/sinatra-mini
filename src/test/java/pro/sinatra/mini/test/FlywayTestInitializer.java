package pro.sinatra.mini.test;

import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import java.util.Arrays;

/**
 * Runs Flyway migrations for tests before the Spring context finishes bootstrap.
 * Registered via test resources (spring.factories) so it runs automatically for tests.
 */
public class FlywayTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        Environment env = context.getEnvironment();
        String url = env.getProperty("spring.datasource.url");
        // Only run this initializer when a Postgres JDBC URL is present.
        // This prevents attempting to open a Postgres driver against an H2 URL.
        boolean looksLikePostgresUrl = url != null && url.startsWith("jdbc:postgresql:");
        if (!looksLikePostgresUrl) {
            return;
        }

        String user = env.getProperty("spring.datasource.username");
        String pass = env.getProperty("spring.datasource.password");
        String driver = env.getProperty("spring.datasource.driver-class-name");
        String locations = env.getProperty("spring.flyway.locations", "classpath:db/migration");
        boolean baselineOnMigrate = Boolean.parseBoolean(env.getProperty("spring.flyway.baseline-on-migrate", "true"));

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        if (user != null) ds.setUsername(user);
        if (pass != null) ds.setPassword(pass);
        if (driver != null) ds.setDriverClassName(driver);

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(ds)
                    .locations(locations)
                    .baselineOnMigrate(baselineOnMigrate)
                    .load();
            flyway.migrate();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to run Flyway migrations for tests", ex);
        } finally {
            try { ds.close(); } catch (Exception ignored) {}
        }
    }
}
