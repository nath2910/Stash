package backend.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backend.entity.EmailVerificationToken;
import backend.entity.User;
import backend.repository.EmailVerificationTokenRepository;
import backend.repository.UserRepository;

@Service
public class EmailVerificationService {

  private final EmailVerificationTokenRepository tokenRepository;
  private final UserRepository userRepository;
  private final JavaMailSender mailSender;
  private final Environment environment;
  private final long expirationMinutes;
  private final String verifyEmailUrl;
  private final String mailFrom;
  private final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);
  private final SecureRandom secureRandom = new SecureRandom();

  public EmailVerificationService(
      EmailVerificationTokenRepository tokenRepository,
      UserRepository userRepository,
      JavaMailSender mailSender,
      Environment environment,
      @Value("${app.email-verification.expiration-minutes:60}") long expirationMinutes,
      @Value("${app.frontend.verify-email-url:http://localhost:5173/verify-email}") String verifyEmailUrl,
      @Value("${app.email-verification.mail-from:}") String mailFrom
  ) {
    this.tokenRepository = tokenRepository;
    this.userRepository = userRepository;
    this.mailSender = mailSender;
    this.environment = environment;
    this.expirationMinutes = expirationMinutes;
    this.verifyEmailUrl = verifyEmailUrl;
    this.mailFrom = mailFrom;
  }

  public void sendVerification(User user) {
    if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
      return;
    }
    if (user.isEmailVerified()) {
      return;
    }
    if (!"LOCAL".equalsIgnoreCase(user.getProvider())) {
      return;
    }

    try {
      ensureMailConfigured();

      tokenRepository.deleteByUserIdAndUsedAtIsNull(user.getId());

      String token = generateToken();
      EmailVerificationToken verificationToken = new EmailVerificationToken();
      verificationToken.setUser(user);
      verificationToken.setToken(token);
      verificationToken.setExpiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)));
      tokenRepository.save(verificationToken);

      String link = buildVerifyLink(token);
      sendVerificationEmail(user.getEmail(), link);
    } catch (Exception ex) {
      logger.warn("Email verification send failed for user {}", user.getId(), ex);
    }
  }

  public void requestVerification(String email) {
    if (email == null || email.isBlank()) {
      return;
    }
    var normalizedEmail = email.trim().toLowerCase();
    var userOpt = userRepository.findByEmail(normalizedEmail);
    if (userOpt.isEmpty()) {
      return;
    }
    sendVerification(userOpt.get());
  }

  public User verifyToken(String token) {
    if (token == null || token.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token manquant");
    }

    EmailVerificationToken verificationToken = tokenRepository.findByToken(token)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien invalide ou expire"));

    if (verificationToken.getUsedAt() != null
        || verificationToken.getExpiresAt().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien invalide ou expire");
    }

    User user = verificationToken.getUser();
    if (user == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien invalide ou expire");
    }

    boolean newlyVerified = !user.isEmailVerified();
    if (newlyVerified) {
      user.setEmailVerified(true);
      userRepository.save(user);
    }

    verificationToken.setUsedAt(Instant.now());
    tokenRepository.save(verificationToken);

    if (newlyVerified) {
      sendConfirmationEmail(user.getEmail());
    }
    return user;
  }

  private void sendVerificationEmail(String to, String link) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);

    String from = resolveFromAddress();
    if (from != null && !from.isBlank()) {
      message.setFrom(from);
    }

    message.setSubject("Verification de votre adresse email");
    message.setText(
        "Bonjour bienvenu sur Stash,\n\n" +
        "Merci de confirmer votre adresse email en cliquant sur le lien ci-dessous :\n" +
        link + "\n\n" +
        "Si vous n'etes pas a l'origine de cette creation de compte, ignorez cet email.\n"
    );

    try {
      mailSender.send(message);
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Erreur envoi email");
    }
  }

  private void sendConfirmationEmail(String to) {
    if (to == null || to.isBlank()) {
      return;
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);

    String from = resolveFromAddress();
    if (from != null && !from.isBlank()) {
      message.setFrom(from);
    }

    message.setSubject("Votre email a ete verifie");
    message.setText(
        "Enfin !\n\n" +
        "Votre adresse email a bien ete confirmee. Vous pouvez maintenant vous connecter.\n\n" +
        "Merci,\n" +
        "L'equipe Stash\n"
    );

    try {
      mailSender.send(message);
    } catch (Exception ex) {
      // Ne bloque pas la validation si l'email de confirmation echoue.
    }
  }

  @Scheduled(fixedDelayString = "${app.email-verification.cleanup-interval-ms:86400000}")
  public void cleanupTokens() {
    try {
      Instant now = Instant.now();
      tokenRepository.deleteByExpiresAtBefore(now);
      tokenRepository.deleteByUsedAtIsNotNull();
    } catch (Exception ex) {
      logger.warn("Email verification cleanup failed", ex);
    }
  }

  private void ensureMailConfigured() {
    String host = environment.getProperty("spring.mail.host");
    if (host == null || host.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service email non configure");
    }
  }

  private String resolveFromAddress() {
    if (mailFrom != null && !mailFrom.isBlank()) {
      return mailFrom;
    }
    return environment.getProperty("spring.mail.username");
  }

  private String buildVerifyLink(String token) {
    if (verifyEmailUrl.contains("{token}")) {
      return verifyEmailUrl.replace("{token}", token);
    }

    return UriComponentsBuilder
        .fromUriString(verifyEmailUrl)
        .queryParam("token", token)
        .toUriString();
  }

  private String generateToken() {
    // 128 bits -> 22 chars Base64URL sans padding (plus court qu'un UUID)
    byte[] bytes = new byte[16];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
