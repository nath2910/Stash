package backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backend.dto.ResetPasswordRequest;
import backend.entity.PasswordResetToken;
import backend.entity.User;
import backend.repository.PasswordResetTokenRepository;
import backend.repository.UserRepository;

@Service
public class PasswordResetService {

  private final UserRepository userRepository;
  private final PasswordResetTokenRepository tokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JavaMailSender mailSender;
  private final Environment environment;
  private final long expirationMinutes;
  private final String resetPasswordUrl;
  private final String mailFrom;
  private final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

  public PasswordResetService(
      UserRepository userRepository,
      PasswordResetTokenRepository tokenRepository,
      PasswordEncoder passwordEncoder,
      JavaMailSender mailSender,
      Environment environment,
      @Value("${app.password-reset.expiration-minutes:60}") long expirationMinutes,
      @Value("${app.frontend.reset-password-url:http://localhost:5173/reset-password}") String resetPasswordUrl,
      @Value("${app.password-reset.mail-from:}") String mailFrom
  ) {
    this.userRepository = userRepository;
    this.tokenRepository = tokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.mailSender = mailSender;
    this.environment = environment;
    this.expirationMinutes = expirationMinutes;
    this.resetPasswordUrl = resetPasswordUrl;
    this.mailFrom = mailFrom;
  }

  public void requestReset(String email) {
    if (email == null || email.isBlank()) {
      logger.info("Password reset skipped: empty email payload");
      return;
    }

    var normalizedEmail = email.trim().toLowerCase();
    var userOpt = userRepository.findByEmail(normalizedEmail);
    if (userOpt.isEmpty()) {
      logger.info("Password reset skipped: no user found for {}", normalizedEmail);
      return;
    }

    User user = userOpt.get();
    if (user.getPassword() == null || !"LOCAL".equalsIgnoreCase(user.getProvider())) {
      logger.info(
          "Password reset skipped: user {} is not eligible (provider={}, hasPassword={})",
          user.getEmail(),
          user.getProvider(),
          user.getPassword() != null
      );
      return;
    }

    ensureMailConfigured();

    String token = UUID.randomUUID().toString().replace("-", "");
    PasswordResetToken resetToken = new PasswordResetToken();
    resetToken.setUser(user);
    resetToken.setToken(token);
    resetToken.setExpiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)));
    tokenRepository.save(resetToken);

    String link = buildResetLink(token);
    sendResetEmail(user.getEmail(), link);
    logger.info("Password reset email queued for {}", user.getEmail());
  }

  public void resetPassword(ResetPasswordRequest request) {
    if (request == null || request.getToken() == null || request.getToken().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token manquant");
    }
    if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mot de passe trop court");
    }

    PasswordResetToken token = tokenRepository.findByToken(request.getToken())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien invalide ou expire"));

    if (token.getUsedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien invalide ou expire");
    }

    User user = token.getUser();
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien invalide ou expire");
    }

    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    token.setUsedAt(Instant.now());
    tokenRepository.save(token);
  }

  private void sendResetEmail(String to, String link) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);

    String from = resolveFromAddress();
    if (from != null && !from.isBlank()) {
      message.setFrom(from);
    }

    message.setSubject("Reinitialisation du mot de passe");
    message.setText(
        "Bonjour,\n\n" +
        "Pour reinitialiser votre mot de passe, cliquez sur le lien ci-dessous :\n" +
        link + "\n\n" +
        "Si vous n'etes pas a l'origine de cette demande, ignorez cet email.\n"
    );

    try {
      mailSender.send(message);
    } catch (Exception ex) {
      logger.warn("Password reset email send failed for {}", to, ex);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Erreur envoi email");
    }
  }

  private void ensureMailConfigured() {
    String host = environment.getProperty("spring.mail.host");
    if (host == null || host.isBlank()) {
      logger.warn("Password reset blocked: spring.mail.host is missing");
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service email non configure");
    }

    String username = environment.getProperty("spring.mail.username");
    if (username == null || username.isBlank()) {
      logger.warn("Password reset blocked: spring.mail.username is missing");
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service email non configure");
    }
  }

  private String resolveFromAddress() {
    if (mailFrom != null && !mailFrom.isBlank()) {
      return mailFrom;
    }
    return environment.getProperty("spring.mail.username");
  }

  private String buildResetLink(String token) {
    return UriComponentsBuilder
        .fromUriString(resetPasswordUrl)
        .queryParam("token", token)
        .toUriString();
  }
}
