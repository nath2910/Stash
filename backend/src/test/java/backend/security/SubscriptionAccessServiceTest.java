package backend.security;

import backend.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class SubscriptionAccessServiceTest {

  private final SubscriptionAccessService service = new SubscriptionAccessService();

  @Test
  void acceptsActiveSubscription() {
    User user = userWithStatus("active");

    Assertions.assertDoesNotThrow(() -> service.requireActiveSubscription(user));
  }

  @Test
  void rejectsInactiveSubscription() {
    User user = userWithStatus("inactive");

    ResponseStatusException ex = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.requireActiveSubscription(user)
    );

    Assertions.assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatusCode());
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
    user.setSubscriptionStatus(status);
    ReflectionTestUtils.setField(user, "id", 5L);
    return user;
  }
}
