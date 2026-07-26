package backend.service;

import backend.entity.EmailVerificationToken;
import backend.entity.User;
import backend.entity.User;
import backend.repository.EmailVerificationTokenRepository;
import backend.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

class EmailVerificationServiceTest {

  @Test
  void sendVerificationPropagatesMailFailure() {
    EmailVerificationTokenRepository tokenRepository = Mockito.mock(EmailVerificationTokenRepository.class);
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.mail.host", "smtp.example.com")
        .withProperty("spring.mail.username", "noreply@example.com");

    EmailVerificationService service = new EmailVerificationService(
        tokenRepository,
        userRepository,
        mailSender,
        environment,
        60,
        "https://mystash.fr/verify-email",
        ""
    );

    User user = Mockito.mock(User.class);
    Mockito.when(user.getId()).thenReturn(42L);
    Mockito.when(user.getEmail()).thenReturn("test@example.com");
    Mockito.when(user.getProvider()).thenReturn("LOCAL");
    Mockito.when(user.isEmailVerified()).thenReturn(false);
    Mockito.doThrow(new MailSendException("smtp failed")).when(mailSender).send(Mockito.any(org.springframework.mail.SimpleMailMessage.class));

    ResponseStatusException exception = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.sendVerification(user)
    );

    Assertions.assertEquals(503, exception.getStatusCode().value());
    Assertions.assertEquals("Erreur envoi email", exception.getReason());
    Mockito.verify(tokenRepository).deleteByUserIdAndUsedAtIsNull(42L);
    Mockito.verify(tokenRepository).save(Mockito.any());
  }

  @Test
  void sendVerificationRejectsMissingSenderConfiguration() {
    EmailVerificationTokenRepository tokenRepository = Mockito.mock(EmailVerificationTokenRepository.class);
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.mail.host", "smtp.example.com");

    EmailVerificationService service = new EmailVerificationService(
        tokenRepository,
        userRepository,
        mailSender,
        environment,
        60,
        "https://mystash.fr/verify-email",
        ""
    );

    User user = Mockito.mock(User.class);
    Mockito.when(user.getEmail()).thenReturn("test@example.com");
    Mockito.when(user.getProvider()).thenReturn("LOCAL");
    Mockito.when(user.isEmailVerified()).thenReturn(false);

    ResponseStatusException exception = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.sendVerification(user)
    );

    Assertions.assertEquals(503, exception.getStatusCode().value());
    Assertions.assertEquals("Service email non configure", exception.getReason());
    Mockito.verifyNoInteractions(mailSender);
    Mockito.verify(tokenRepository, Mockito.never()).save(Mockito.any());
  }

  @Test
  void verifyTokenMarksUserAndTokenAsVerified() {
    EmailVerificationTokenRepository tokenRepository = Mockito.mock(EmailVerificationTokenRepository.class);
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
    MockEnvironment environment = new MockEnvironment()
        .withProperty("spring.mail.host", "smtp.example.com")
        .withProperty("spring.mail.username", "noreply@example.com");

    EmailVerificationService service = new EmailVerificationService(
        tokenRepository,
        userRepository,
        mailSender,
        environment,
        60,
        "https://mystash.fr/verify-email",
        ""
    );

    User user = new User();
    user.setEmail("test@example.com");
    user.setProvider("LOCAL");
    user.setEmailVerified(false);

    EmailVerificationToken token = new EmailVerificationToken();
    token.setToken("valid-token");
    token.setUser(user);
    token.setExpiresAt(Instant.now().plusSeconds(3600));

    Mockito.when(tokenRepository.findByToken("valid-token")).thenReturn(java.util.Optional.of(token));

    User verifiedUser = service.verifyToken("valid-token");

    Assertions.assertSame(user, verifiedUser);
    Assertions.assertTrue(user.isEmailVerified());
    Assertions.assertNotNull(token.getUsedAt());
    Mockito.verify(userRepository).save(user);
    Mockito.verify(tokenRepository, Mockito.atLeastOnce()).save(token);
    Mockito.verify(mailSender).send(Mockito.any(org.springframework.mail.SimpleMailMessage.class));
  }

  @Test
  void verifyTokenRejectsExpiredTokenWithBadRequest() {
    EmailVerificationTokenRepository tokenRepository = Mockito.mock(EmailVerificationTokenRepository.class);
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
    MockEnvironment environment = new MockEnvironment();

    EmailVerificationService service = new EmailVerificationService(
        tokenRepository,
        userRepository,
        mailSender,
        environment,
        60,
        "https://mystash.fr/verify-email",
        ""
    );

    User user = new User();
    user.setEmail("test@example.com");

    EmailVerificationToken token = new EmailVerificationToken();
    token.setToken("expired-token");
    token.setUser(user);
    token.setExpiresAt(Instant.now().minusSeconds(60));

    Mockito.when(tokenRepository.findByToken("expired-token")).thenReturn(java.util.Optional.of(token));

    ResponseStatusException exception = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.verifyToken("expired-token")
    );

    Assertions.assertEquals(400, exception.getStatusCode().value());
    Assertions.assertEquals("Lien invalide ou expire", exception.getReason());
    Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
  }
}
