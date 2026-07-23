package backend.service;

import backend.config.StripeProperties;
import backend.entity.User;
import backend.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BillingServiceTest {

  @Test
  void refreshStatusKeepsStoredValueWhenNoStripeCustomerIdExists() {
    StripeProperties properties = new StripeProperties();
    properties.setSecretKey("sk_test_123");
    properties.setPriceId("price_123");
    properties.setSuccessUrl("https://mystash.fr/abo?success=1");

    UserRepository userRepository = Mockito.mock(UserRepository.class);
    BillingService service = new BillingService(properties, userRepository);

    User user = Mockito.mock(User.class);
    Mockito.when(user.getSubscriptionStatus()).thenReturn("inactive");
    Mockito.when(user.getStripeCustomerId()).thenReturn(null);

    String status = service.refreshStatus(user);

    Assertions.assertEquals("inactive", status);
    Mockito.verify(user, Mockito.never()).setStripeCustomerId(Mockito.anyString());
    Mockito.verifyNoInteractions(userRepository);
  }
}
