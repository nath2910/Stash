package backend.controller;

import backend.dto.BillingStatusResponse;
import backend.dto.CheckoutRequest;
import backend.dto.CheckoutResponse;
import backend.entity.User;
import backend.service.BillingService;
import com.stripe.model.billingportal.Session;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class BillingControllerTest {

  @Mock
  private BillingService billingService;

  private BillingController controller;
  private User user;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    controller = new BillingController(billingService);
    user = Mockito.mock(User.class);
    Mockito.when(user.getId()).thenReturn(42L);
    Mockito.when(user.getSubscriptionStatus()).thenReturn("inactive");
  }

  @Test
  void statusReturns503WhenStripeIsNotConfigured() {
    Mockito.when(billingService.isConfigured()).thenReturn(false);

    ResponseStatusException ex = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> controller.status(user, false)
    );

    Assertions.assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
  }

  @Test
  void statusReturnsBillingSnapshotWhenConfigured() throws Exception {
    Mockito.when(billingService.isConfigured()).thenReturn(true);
    Mockito.when(user.getSubscriptionStatus()).thenReturn("active");
    Session portal = Mockito.mock(Session.class);
    Mockito.when(portal.getUrl()).thenReturn("https://stripe.test/portal");
    Mockito.when(billingService.createPortal(user)).thenReturn(portal);

    BillingStatusResponse response = controller.status(user, true);

    Mockito.verify(billingService).refreshStatus(user);
    Assertions.assertEquals("active", response.status());
    Assertions.assertEquals("https://stripe.test/portal", response.portalUrl());
  }

  @Test
  void checkoutReturns503WhenStripeIsNotConfigured() {
    Mockito.when(billingService.isConfigured()).thenReturn(false);

    ResponseStatusException ex = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> controller.checkout(user, new CheckoutRequest(null, null))
    );

    Assertions.assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
  }

  @Test
  void checkoutReturnsSessionUrlWhenConfigured() throws Exception {
    Mockito.when(billingService.isConfigured()).thenReturn(true);
    com.stripe.model.checkout.Session session = Mockito.mock(com.stripe.model.checkout.Session.class);
    Mockito.when(session.getUrl()).thenReturn("https://stripe.test/checkout");
    Mockito.when(billingService.createCheckout(user, "PROMO", "123")).thenReturn(session);

    CheckoutResponse response = controller.checkout(user, new CheckoutRequest("PROMO", "123"));

    Assertions.assertEquals("https://stripe.test/checkout", response.url());
  }

  @Test
  void checkoutSanitizesUnexpectedProviderFailures() throws Exception {
    Mockito.when(billingService.isConfigured()).thenReturn(true);
    Mockito.when(billingService.createCheckout(user, null, null))
        .thenThrow(new IllegalStateException("Stripe low-level error"));

    ResponseStatusException ex = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> controller.checkout(user, new CheckoutRequest(null, null))
    );

    Assertions.assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatusCode());
    Assertions.assertEquals("Session de paiement indisponible", ex.getReason());
  }
}
