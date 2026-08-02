package backend.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProductionReadinessValidatorTest {

  @Test
  void acceptsStrongProductionJwtSecret() {
    Assertions.assertDoesNotThrow(() ->
        ProductionReadinessValidator.validateJwtSecret("prod-super-secret-key-0123456789-abcdef"));
  }

  @Test
  void rejectsDefaultJwtSecretForProduction() {
    IllegalStateException ex = Assertions.assertThrows(
        IllegalStateException.class,
        () -> ProductionReadinessValidator.validateJwtSecret(
            ProductionReadinessValidator.DEFAULT_DEV_JWT_SECRET)
    );

    Assertions.assertTrue(ex.getMessage().contains("JWT_SECRET"));
  }

  @Test
  void acceptsLocalhostCorsOriginsForProduction() {
    Assertions.assertDoesNotThrow(() ->
        ProductionReadinessValidator.validateCorsOrigins(
            "https://mystash.fr,http://localhost:5173,https://preview.localhost:4173"));
  }

  @Test
  void rejectsNonHttpsPublicCorsOriginsForProduction() {
    IllegalStateException ex = Assertions.assertThrows(
        IllegalStateException.class,
        () -> ProductionReadinessValidator.validateCorsOrigins(
            "https://mystash.fr,http://frontend.mystash.fr")
    );

    Assertions.assertTrue(ex.getMessage().contains("APP_CORS_ALLOWED_ORIGINS"));
  }

  @Test
  void rejectsNonHttpsFrontendUrlForProduction() {
    IllegalStateException ex = Assertions.assertThrows(
        IllegalStateException.class,
        () -> ProductionReadinessValidator.validatePublicHttpsUrl(
            "http://mystash.fr",
            "APP_FRONTEND_BASE_URL")
    );

    Assertions.assertTrue(ex.getMessage().contains("https"));
  }
}
