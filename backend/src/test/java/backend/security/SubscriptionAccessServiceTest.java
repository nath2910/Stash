package backend.security;

import backend.entity.User;
import backend.service.DiscordAccessService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class SubscriptionAccessServiceTest {

  private final DiscordAccessService discordAccessService = Mockito.mock(DiscordAccessService.class);
  private final SubscriptionAccessService service = new SubscriptionAccessService(discordAccessService);

  @Test
  void acceptsActiveSubscription() {
    User user = userWithStatus("active");
    Mockito.when(discordAccessService.isEligible(Mockito.any())).thenReturn(false);

    Assertions.assertDoesNotThrow(() -> service.requireActiveSubscription(user));
  }

  @Test
  void acceptsActiveSubscriptionWhenStripePeriodEndIsMissing() {
    User user = userWithStatus("active");
    user.setSubscriptionCurrentPeriodEnd(null);
    Mockito.when(discordAccessService.isEligible(Mockito.any())).thenReturn(false);

    Assertions.assertDoesNotThrow(() -> service.requireActiveSubscription(user));
  }

  @Test
  void rejectsInactiveSubscription() {
    User user = userWithStatus("inactive");
    Mockito.when(discordAccessService.isEligible(Mockito.any())).thenReturn(false);

    ResponseStatusException ex = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.requireActiveSubscription(user)
    );

    Assertions.assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatusCode());
  }

  @Test
  void allowsDiscordEligibleUserWithoutStripeSubscription() {
    User user = userWithStatus("inactive");
    Mockito.when(discordAccessService.isEligible(Mockito.any())).thenReturn(true);

    Assertions.assertDoesNotThrow(() -> service.requireActiveSubscription(user));
  }

  @Test
  void rejectsMissingUser() {
    ResponseStatusException ex = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.requireActiveSubscription(null)
    );

    Assertions.assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
  }

  private User userWithStatus(String status) {
    User user = new User();
    user.setSubscriptionCurrentPeriodEnd(java.time.OffsetDateTime.now().plusDays(1));
    user.setSubscriptionStatus(status);
    ReflectionTestUtils.setField(user, "id", 5L);
    return user;
  }
}
