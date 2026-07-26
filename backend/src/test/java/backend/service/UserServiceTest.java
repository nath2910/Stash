package backend.service;

import backend.dto.RegisterRequest;
import backend.entity.User;
import backend.repository.EmailVerificationTokenRepository;
import backend.repository.PasswordResetTokenRepository;
import backend.repository.SnkVenteRepository;
import backend.repository.UserRepository;
import backend.repository.UserStatsLayoutRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

class UserServiceTest {

  @Test
  void registerRollsBackUserWhenVerificationEmailFails() {
    UserRepository userRepository = Mockito.mock(UserRepository.class);
    PasswordEncoder passwordEncoder = Mockito.mock(PasswordEncoder.class);
    EmailVerificationService emailVerificationService = Mockito.mock(EmailVerificationService.class);
    EmailVerificationTokenRepository emailVerificationTokenRepository = Mockito.mock(EmailVerificationTokenRepository.class);
    PasswordResetTokenRepository passwordResetTokenRepository = Mockito.mock(PasswordResetTokenRepository.class);
    UserStatsLayoutRepository userStatsLayoutRepository = Mockito.mock(UserStatsLayoutRepository.class);
    SnkVenteRepository snkVenteRepository = Mockito.mock(SnkVenteRepository.class);

    UserService service = new UserService(
        userRepository,
        passwordEncoder,
        emailVerificationService,
        emailVerificationTokenRepository,
        passwordResetTokenRepository,
        userStatsLayoutRepository,
        snkVenteRepository
    );

    RegisterRequest request = new RegisterRequest();
    request.setEmail("Test@Example.com");
    request.setPassword("secret123");
    request.setFirstName("Ada");
    request.setLastName("Lovelace");

    Mockito.when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
    Mockito.when(passwordEncoder.encode("secret123")).thenReturn("hashed-password");
    Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> {
      User saved = invocation.getArgument(0);
      ReflectionTestUtils.setField(saved, "id", 99L);
      return saved;
    });
    Mockito.doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Erreur envoi email"))
        .when(emailVerificationService)
        .sendVerification(Mockito.any(User.class));

    ResponseStatusException exception = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.register(request)
    );

    Assertions.assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    Assertions.assertEquals("Erreur envoi email", exception.getReason());

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    Mockito.verify(userRepository).save(captor.capture());
    Assertions.assertEquals("test@example.com", captor.getValue().getEmail());
    Assertions.assertEquals("hashed-password", captor.getValue().getPassword());
    Mockito.verify(emailVerificationTokenRepository).deleteByUserId(99L);
    Mockito.verify(userRepository).deleteById(99L);
  }
}
