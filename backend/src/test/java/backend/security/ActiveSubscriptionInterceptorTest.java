package backend.security;

import backend.entity.User;
import backend.service.BillingService;
import backend.service.DiscordAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.server.ResponseStatusException;

class ActiveSubscriptionInterceptorTest {

  private final DiscordAccessService discordAccessService = Mockito.mock(DiscordAccessService.class);
  private final BillingService billingService = Mockito.mock(BillingService.class);
  private final SubscriptionAccessService service = new SubscriptionAccessService(discordAccessService);
  private final ActiveSubscriptionInterceptor interceptor = new ActiveSubscriptionInterceptor(service, billingService);

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void allowsAnnotatedHandlerForActiveUser() throws Exception {
    setAuthenticatedUser("active");
    Mockito.when(discordAccessService.isEligible(Mockito.any())).thenReturn(false);

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
    Mockito.when(discordAccessService.isEligible(Mockito.any())).thenReturn(false);

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
  void refreshesStripeBeforeBlockingProtectedHandler() throws Exception {
    User user = setAuthenticatedUser("inactive");
    user.setStripeCustomerId("cus_123");
    Mockito.when(discordAccessService.isEligible(Mockito.any())).thenReturn(false);
    Mockito.doAnswer(invocation -> {
      User refreshed = invocation.getArgument(0);
      refreshed.setSubscriptionStatus("active");
      refreshed.setSubscriptionCurrentPeriodEnd(java.time.OffsetDateTime.now().plusDays(1));
      return "active";
    }).when(billingService).refreshStatus(user);

    boolean allowed = interceptor.preHandle(
        new MockHttpServletRequest(),
        new MockHttpServletResponse(),
        new HandlerMethod(new PremiumHandler(), PremiumHandler.class.getMethod("premium"))
    );

    Assertions.assertTrue(allowed);
    Mockito.verify(billingService).refreshStatus(user);
  }

  @Test
  void allowsAnnotatedHandlerForDiscordEligibleUser() throws Exception {
    setAuthenticatedUser("inactive");
    Mockito.when(discordAccessService.isEligible(Mockito.any())).thenReturn(true);

    boolean allowed = interceptor.preHandle(
        new MockHttpServletRequest(),
        new MockHttpServletResponse(),
        new HandlerMethod(new PremiumHandler(), PremiumHandler.class.getMethod("premium"))
    );

    Assertions.assertTrue(allowed);
  }

  @Test
  void ignoresUnannotatedHandler() throws Exception {
    setAuthenticatedUser("inactive");
    Mockito.when(discordAccessService.isEligible(Mockito.any())).thenReturn(false);

    boolean allowed = interceptor.preHandle(
        new MockHttpServletRequest(),
        new MockHttpServletResponse(),
        new HandlerMethod(new PublicHandler(), PublicHandler.class.getMethod("publicEndpoint"))
    );

    Assertions.assertTrue(allowed);
  }

  private User setAuthenticatedUser(String subscriptionStatus) {
    User user = new User();
    user.setSubscriptionCurrentPeriodEnd(java.time.OffsetDateTime.now().plusDays(1));
    user.setSubscriptionStatus(subscriptionStatus);
    ReflectionTestUtils.setField(user, "id", 8L);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user, null)
    );
    return user;
  }

  @RequiresActiveSubscription
  static class PremiumHandler {
    public void premium() {}
  }

  static class PublicHandler {
    public void publicEndpoint() {}
  }
}
