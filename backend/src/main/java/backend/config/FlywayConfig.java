package backend.config;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;

@Configuration
public class FlywayConfig {

  @Bean
  public FlywayMigrationStrategy migrateStrategy() {
    // A checksum mismatch is a deployment error. Repair requires a reviewed operator action.
    return Flyway::migrate;
  }
}
