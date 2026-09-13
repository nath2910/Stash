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

  @Test
  void rejectsStripeTestSecretForProduction() {
    StripeProperties stripe = validProductionStripe();
    stripe.setSecretKey("sk_test_123");

    IllegalStateException ex = Assertions.assertThrows(
        IllegalStateException.class,
        () -> ProductionReadinessValidator.validateStripeProductionConfiguration(stripe)
    );

    Assertions.assertTrue(ex.getMessage().contains("STRIPE_SECRET_KEY"));
  }

  @Test
  void rejectsDisabledStripeSalesForProduction() {
    StripeProperties stripe = validProductionStripe();
    stripe.setSalesEnabled(false);

    IllegalStateException ex = Assertions.assertThrows(
        IllegalStateException.class,
        () -> ProductionReadinessValidator.validateStripeProductionConfiguration(stripe)
    );

    Assertions.assertTrue(ex.getMessage().contains("STRIPE_SALES_ENABLED"));
  }

  @Test
  void acceptsCompleteStripeLiveConfigurationForProduction() {
    Assertions.assertDoesNotThrow(() ->
        ProductionReadinessValidator.validateStripeProductionConfiguration(validProductionStripe()));
  }

  private StripeProperties validProductionStripe() {
    StripeProperties stripe = new StripeProperties();
    stripe.setSecretKey("sk_live_123");
    stripe.setPriceId("price_monthly");
    stripe.setWebhookSecret("whsec_live");
    stripe.setSuccessUrl("https://mystash.fr/abo?success=1");
    stripe.setCancelUrl("https://mystash.fr/abo?canceled=1");
    stripe.setSalesEnabled(true);
    stripe.setCommercialRegistrationComplete(true);
    return stripe;
  }
}
