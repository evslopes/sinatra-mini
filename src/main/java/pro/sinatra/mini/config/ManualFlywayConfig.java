package pro.sinatra.mini.config;

import org.flywaydb.core.Flyway;
import jakarta.sql.DataSource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ManualFlywayConfig {

    private final ApplicationContext context;

    public ManualFlywayConfig(ApplicationContext context) {
        this.context = context;
    }

    @Bean
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrateAfterStartup() {
        Flyway flyway = context.getBean(Flyway.class);
        flyway.migrate();
    }
}

