package backend.service;

import backend.config.StripeProperties;
import backend.dto.CheckoutRequest;
import backend.entity.User;
import backend.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class BillingServiceTest {

  @Test
  void refreshStatusKeepsStoredValueWhenNoStripeCustomerIdExists() {
    StripeProperties properties = new StripeProperties();
    properties.setSecretKey("sk_test_123");
    properties.setPriceId("price_123");
    properties.setWebhookSecret("whsec_test");
    properties.setSuccessUrl("https://mystash.fr/abo?success=1");

    UserRepository userRepository = Mockito.mock(UserRepository.class);
    BillingService service = new BillingService(properties, userRepository, Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class), new com.fasterxml.jackson.databind.ObjectMapper());

    User user = Mockito.mock(User.class);
    Mockito.when(user.getSubscriptionStatus()).thenReturn("inactive");
    Mockito.when(user.getStripeCustomerId()).thenReturn(null);

    String status = service.refreshStatus(user);

    Assertions.assertEquals("inactive", status);
    Mockito.verify(user, Mockito.never()).setStripeCustomerId(Mockito.anyString());
    Mockito.verifyNoInteractions(userRepository);
  }

  @Test
  void liveCheckoutStaysClosedUntilCommercialRegistrationIsComplete() {
    StripeProperties properties = new StripeProperties();
    properties.setSecretKey("sk_live_123");
    properties.setPriceId("price_123");
    properties.setWebhookSecret("whsec_test");
    properties.setSuccessUrl("https://mystash.fr/abo?success=1");
    properties.setSalesEnabled(true);
    properties.setCommercialRegistrationComplete(false);

    UserRepository userRepository = Mockito.mock(UserRepository.class);
    BillingService service = new BillingService(
        properties,
        userRepository,
        Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class),
        new com.fasterxml.jackson.databind.ObjectMapper()
    );

    ResponseStatusException exception = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.createCheckout(new User(), new CheckoutRequest("monthly", true))
    );

    Assertions.assertEquals(503, exception.getStatusCode().value());
    Assertions.assertEquals("Souscriptions bientôt disponibles", exception.getReason());
    Mockito.verifyNoInteractions(userRepository);
  }

  @Test
  void testCheckoutDoesNotRequireCommercialRegistration() {
    StripeProperties properties = new StripeProperties();
    properties.setSecretKey("sk_test_123");
    properties.setPriceId("price_123");
    properties.setWebhookSecret("whsec_test");
    properties.setSuccessUrl("https://mystash.fr/abo?success=1");

    UserRepository userRepository = Mockito.mock(UserRepository.class);
    BillingService service = new BillingService(
        properties,
        userRepository,
        Mockito.mock(org.springframework.jdbc.core.JdbcTemplate.class),
        new com.fasterxml.jackson.databind.ObjectMapper()
    );

    ResponseStatusException exception = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.createCheckout(new User(), new CheckoutRequest("monthly", true))
    );

    Assertions.assertEquals(401, exception.getStatusCode().value());
    Mockito.verify(userRepository).lockById(Mockito.isNull());
  }
}
