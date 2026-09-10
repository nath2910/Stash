package backend.service;

import backend.security.SensitiveTokenHasher;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    ensureMailConfigured();

    tokenRepository.deleteByUserIdAndUsedAtIsNull(user.getId());

    String token = generateToken();
    String tokenHash = SensitiveTokenHasher.hash(token);
    EmailVerificationToken verificationToken = new EmailVerificationToken();
    verificationToken.setUser(user);
    verificationToken.setToken(tokenHash);
    verificationToken.setExpiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)));
    tokenRepository.save(verificationToken);

    String link = buildVerifyLink(token, user.getEmail());
    sendVerificationEmail(user.getEmail(), link);
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

  @Transactional
  public User verifyToken(String token) {
    if (token == null || token.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token manquant");
    }

    EmailVerificationToken verificationToken = tokenRepository.findByToken(SensitiveTokenHasher.hash(token))
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
    String from = resolveFromAddress();

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(to);
      helper.setFrom(from, "MyStash");
      helper.setSubject("Confirme ton adresse email MyStash");
      helper.setText(
          "Bienvenue sur MyStash.\n\n"
              + "Confirme ton adresse email pour activer ton compte :\n"
              + link + "\n\n"
              + "Ce lien expire dans " + expirationMinutes + " minutes.\n"
              + "Si tu n'es pas a l'origine de cette creation de compte, ignore cet email.\n",
          verificationHtml(link)
      );
      mailSender.send(message);
    } catch (Exception ex) {
      logger.warn("Verification email send failed for {}", to, ex);
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Erreur envoi email");
    }
  }

  private void sendConfirmationEmail(String to) {
    if (to == null || to.isBlank()) {
      return;
    }

    String from = resolveFromAddress();

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(to);
      helper.setFrom(from, "MyStash");
      helper.setSubject("Ton compte MyStash est active");
      helper.setText(
          "Ton adresse email a bien ete confirmee. Tu peux maintenant te connecter a MyStash.\n",
          confirmationHtml()
      );
      mailSender.send(message);
    } catch (Exception ex) {
      // Ne bloque pas la validation si l'email de confirmation echoue.
      logger.warn("Verification confirmation email failed for {}", to, ex);
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
      logger.warn("Email verification blocked: spring.mail.host is missing");
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service email non configure");
    }

    String from = resolveFromAddress();
    if (from == null || from.isBlank()) {
      logger.warn("Email verification blocked: sender address is missing");
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service email non configure");
    }
  }

  private String resolveFromAddress() {
    if (mailFrom != null && !mailFrom.isBlank()) {
      return mailFrom;
    }
    return environment.getProperty("spring.mail.username");
  }

  private String buildVerifyLink(String token, String email) {
    if (verifyEmailUrl.contains("{token}")) {
      return UriComponentsBuilder
          .fromUriString(verifyEmailUrl.replace("{token}", token))
          .queryParam("email", email)
          .toUriString();
    }

    return UriComponentsBuilder
        .fromUriString(verifyEmailUrl)
        .queryParam("token", token)
        .queryParam("email", email)
        .toUriString();
  }

  private String generateToken() {
    // 128 bits -> 22 chars Base64URL sans padding (plus court qu'un UUID)
    byte[] bytes = new byte[16];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String verificationHtml(String link) {
    String safeLink = escapeHtml(link);
    return baseEmailHtml(
        "Confirme ton adresse email",
        "Pour activer ton compte MyStash, ouvre ce lien dans ton navigateur.",
        "Confirmer mon email",
        safeLink,
        "Ce lien expire dans " + expirationMinutes + " minutes. Si tu n'as pas cree de compte MyStash, tu peux ignorer cet email.",
        true,
        true
    );
  }

  private String confirmationHtml() {
    return baseEmailHtml(
        "Ton compte est active",
        "Ton adresse email est confirmee. Ton espace MyStash est pret.",
        "Ouvrir MyStash",
        escapeHtml(environment.getProperty("app.frontend.base-url", "https://mystash.fr")),
        "Tu peux maintenant te connecter et commencer a utiliser ton compte.",
        false,
        false
    );
  }

  private String baseEmailHtml(
      String title,
      String intro,
      String buttonLabel,
      String link,
      String footnote,
      boolean sensitiveAction,
      boolean showFallbackLink
  ) {
    String securityBlock = sensitiveAction
        ? "<p style=\"margin:22px 0 0;font-size:16px;line-height:1.65;color:#4f566b;\">Ne partage jamais ce lien. L'equipe MyStash ne te demandera jamais de le copier sur un autre site.</p>"
        : "";
    String fallbackBlock = showFallbackLink
        ? "<p style=\"margin:34px 0 0;font-size:14px;line-height:1.7;color:#697386;\">Si le bouton ne fonctionne pas, copie ce lien dans ton navigateur :</p><p style=\"margin:8px 0 0;font-size:13px;line-height:1.55;word-break:break-all;color:#635bff;\">%s</p>".formatted(link)
        : "";
    String brandBlock = emailBrandHtml();
    return """
        <!doctype html>
        <html>
          <body style="margin:0;background:#f6f9fc;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Arial,sans-serif;color:#3c4257;">
            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#f6f9fc;padding:54px 16px;">
              <tr>
                <td align="center">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:720px;">
                    <tr>
                      <td style="background:#ffffff;border-radius:14px;padding:58px 64px 46px;box-shadow:0 1px 2px rgba(60,66,87,0.08);">
                        %s
                        <h1 style="margin:42px 0 0;font-size:24px;line-height:1.35;font-weight:700;color:#30313d;">%s</h1>
                        <p style="margin:24px 0 0;font-size:17px;line-height:1.65;color:#4f566b;">%s</p>
                        %s
                        <p style="margin:30px 0 0;">
                          <a href="%s" style="display:inline-block;background:#635bff;color:#ffffff;text-decoration:none;font-weight:700;font-size:16px;border-radius:7px;padding:12px 20px;">%s</a>
                        </p>
                        %s
                        <div style="height:1px;background:#e6ebf1;margin:34px 0 0;"></div>
                        <p style="margin:28px 0 0;font-size:15px;line-height:1.7;color:#4f566b;">%s</p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:24px 0 0;">
                        <p style="margin:0;font-size:13px;line-height:1.7;color:#697386;">MyStash - mystash.fr</p>
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """.formatted(
        brandBlock,
        escapeHtml(title),
        escapeHtml(intro),
        securityBlock,
        link,
        escapeHtml(buttonLabel),
        fallbackBlock,
        escapeHtml(footnote)
    );
  }

  private String emailBrandHtml() {
    String logoUrl = environment.getProperty("app.email-brand.logo-url", "");
    if (logoUrl == null || logoUrl.isBlank() || !logoUrl.startsWith("https://")) {
      return "<div style=\"text-align:center;font-size:22px;font-weight:800;letter-spacing:-0.02em;color:#0f172a;\">MyStash</div>";
    }
    return "<div style=\"text-align:center;line-height:0;\"><img src=\"%s\" width=\"128\" alt=\"MyStash\" style=\"display:inline-block;width:128px;max-width:100%%;height:auto;border:0;outline:none;text-decoration:none;\"></div>"
        .formatted(escapeHtml(logoUrl.trim()));
  }

  private String escapeHtml(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }
}
