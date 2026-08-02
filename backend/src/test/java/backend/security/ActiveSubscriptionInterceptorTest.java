package backend.security;

import backend.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;

class ActiveSubscriptionInterceptorTest {

  private final ActiveSubscriptionInterceptor interceptor =
      new ActiveSubscriptionInterceptor(new SubscriptionAccessService());

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void allowsAnnotatedHandlerForActiveUser() throws Exception {
    setAuthenticatedUser("active");

    boolean allowed = interceptor.preHandle(
        new MockHttpServletRequest(),
        new MockHttpServletResponse(),
        new HandlerMethod(new PremiumHandler(), PremiumHandler.class.getMethod("premium"))
    );

    Assertions.assertTrue(allowed);
  }

  @Test
  void blocksAnnotatedHandlerForInactiveUser() throws Exception {
    setAuthenticatedUser("inactive");

    ResponseStatusException ex = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> interceptor.preHandle(
            new MockHttpServletRequest(),
            new MockHttpServletResponse(),
            new HandlerMethod(new PremiumHandler(), PremiumHandler.class.getMethod("premium"))
        )
    );

    Assertions.assertEquals(402, ex.getStatusCode().value());
  }

  @Test
  void ignoresUnannotatedHandler() throws Exception {
    setAuthenticatedUser("inactive");

    boolean allowed = interceptor.preHandle(
        new MockHttpServletRequest(),
        new MockHttpServletResponse(),
        new HandlerMethod(new PublicHandler(), PublicHandler.class.getMethod("publicEndpoint"))
    );

    Assertions.assertTrue(allowed);
  }

  private void setAuthenticatedUser(String subscriptionStatus) {
    User user = new User();
    user.setSubscriptionStatus(subscriptionStatus);
    ReflectionTestUtils.setField(user, "id", 8L);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null)
    );
  }

  @RequiresActiveSubscription
  static class PremiumHandler {
    public void premium() {}
  }

  static class PublicHandler {
    public void publicEndpoint() {}
  }
}
