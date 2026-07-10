package backend.service;

import backend.repository.MailAccountRepository;
import backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.env.MockEnvironment;

class GmailOAuthServiceTest {

  @Test
  void rejectsLocalhostRedirectWhenProdAndGmailOauthConfigured() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");

    GmailOAuthService service = new GmailOAuthService(
        "google-client-id",
        "google-client-secret",
        "http://localhost:8080/delivery/mail-accounts/gmail/callback",
        "jwt-secret-for-tests",
        "https://mystash.fr",
        "https://mystash.fr/auth/callback",
        new ObjectMapper(),
        environment,
        Mockito.mock(UserRepository.class),
        Mockito.mock(MailAccountRepository.class),
        Mockito.mock(TokenEncryptionService.class)
    );

    IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, service::validateProductionConfig);
    Assertions.assertTrue(exception.getMessage().contains("APP_DELIVERY_GMAIL_REDIRECT_URI"));
  }

  @Test
  void acceptsPublicHttpsUrlsWhenProdAndGmailOauthConfigured() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");

    GmailOAuthService service = new GmailOAuthService(
        "google-client-id",
        "google-client-secret",
        "https://api.mystash.fr/delivery/mail-accounts/gmail/callback",
        "jwt-secret-for-tests",
        "https://mystash.fr",
        "https://mystash.fr/auth/callback",
        new ObjectMapper(),
        environment,
        Mockito.mock(UserRepository.class),
        Mockito.mock(MailAccountRepository.class),
        Mockito.mock(TokenEncryptionService.class)
    );

    Assertions.assertDoesNotThrow(service::validateProductionConfig);
  }
}
