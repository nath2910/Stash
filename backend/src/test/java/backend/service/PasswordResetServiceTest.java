package backend.service;

import backend.dto.ResetPasswordRequest;
import backend.entity.PasswordResetToken;
import backend.entity.User;
import backend.repository.PasswordResetTokenRepository;
import backend.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class PasswordResetServiceTest {

  @Test
  void resetPasswordUpdatesUserAndMarksTokenUsed() {
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    PasswordResetTokenRepository tokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
    PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
    MockEnvironment environment = new MockEnvironment();

    PasswordResetService service = new PasswordResetService(
        userRepository,
        tokenRepository,
        passwordEncoder,
        mailSender,
        environment,
        60,
        "https://mystash.fr/reset-password",
        ""
    );

    User user = new User();
    user.setEmail("test@example.com");
    user.setPassword("old-password");

    PasswordResetToken token = new PasswordResetToken();
    token.setToken("reset-token");
    token.setUser(user);
    token.setExpiresAt(Instant.now().plusSeconds(3600));

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("reset-token");
    request.setNewPassword("new-password");

    Mockito.when(tokenRepository.findByToken("reset-token")).thenReturn(Optional.of(token));
    Mockito.when(passwordEncoder.encode("new-password")).thenReturn("encoded-password");

    service.resetPassword(request);

    Assertions.assertEquals("encoded-password", user.getPassword());
    Assertions.assertNotNull(token.getUsedAt());
    Mockito.verify(userRepository).save(user);
    Mockito.verify(tokenRepository).save(token);
  }

  @Test
  void resetPasswordRejectsInvalidToken() {
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    PasswordResetTokenRepository tokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
    PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
    MockEnvironment environment = new MockEnvironment();

    PasswordResetService service = new PasswordResetService(
        userRepository,
        tokenRepository,
        passwordEncoder,
        mailSender,
        environment,
        60,
        "https://mystash.fr/reset-password",
        ""
    );

    ResetPasswordRequest request = new ResetPasswordRequest();
    request.setToken("missing-token");
    request.setNewPassword("new-password");

    Mockito.when(tokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

    ResponseStatusException exception = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.resetPassword(request)
    );

    Assertions.assertEquals(400, exception.getStatusCode().value());
    Assertions.assertEquals("Lien invalide ou expire", exception.getReason());
    Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
  }
}
