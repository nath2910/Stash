package backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

@Configuration
public class FlywayConfig {

  @Bean
  public FlywayMigrationStrategy repairThenMigrateStrategy() {
    return flyway -> {
      try {
        flyway.repair();
      } catch (Exception ignored) {}
      flyway.migrate();
    };
  }
}