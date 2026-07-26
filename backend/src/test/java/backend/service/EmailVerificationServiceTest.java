package backend.service;

import backend.entity.User;
import backend.repository.EmailVerificationTokenRepository;
import backend.repository.UserRepository;
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
}
