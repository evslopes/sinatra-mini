package pro.sinatra.mini.config;

import org.flywaydb.core.Flyway;
import javax.sql.DataSource;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@ConditionalOnProperty(name = "sinatra.manual-flyway.enabled", havingValue = "true", matchIfMissing = false)
public class ManualFlywayConfig {
    private final DataSource dataSource;

    public ManualFlywayConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrateAfterStartup() {
        // create a local Flyway instance (do NOT register bean named 'flyway')
        Flyway.configure()
            .dataSource(this.dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
            .migrate();
    }
}

