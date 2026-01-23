package pro.sinatra.mini;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;

@SpringBootApplication(exclude = {FlywayAutoConfiguration.class})
public class MiniApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiniApplication.class, args);
    }
}