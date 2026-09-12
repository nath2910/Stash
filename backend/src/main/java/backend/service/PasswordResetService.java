package backend.service;

import backend.security.PasswordPolicy;
import backend.security.SensitiveTokenHasher;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    if (user.getPassword() == null || user.getPassword().isBlank()) {
      logger.info(
          "Password reset skipped: user {} is not eligible (provider={}, hasPassword={})",
          user.getEmail(),
          user.getProvider(),
          user.getPassword() != null && !user.getPassword().isBlank()
      );
      return;
    }

    ensureMailConfigured();

    String token = UUID.randomUUID().toString().replace("-", "");
    String tokenHash = SensitiveTokenHasher.hash(token);
    PasswordResetToken resetToken = new PasswordResetToken();
    resetToken.setUser(user);
    resetToken.setToken(tokenHash);
    resetToken.setExpiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)));
    tokenRepository.save(resetToken);

    String link = buildResetLink(token);
    sendResetEmail(user.getEmail(), link);
    logger.info("Password reset email queued for {}", user.getEmail());
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    if (request == null || request.getToken() == null || request.getToken().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token manquant");
    }
    if (PasswordPolicy.isTooShort(request.getNewPassword())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Mot de passe trop court (minimum " + PasswordPolicy.MIN_LENGTH + " caractères)"
      );
    }

    PasswordResetToken token = tokenRepository.findByToken(SensitiveTokenHasher.hash(request.getToken()))
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
    userRepository.revokeSessions(user.getId());

    token.setUsedAt(Instant.now());
    tokenRepository.save(token);
  }

  private void sendResetEmail(String to, String link) {
    String from = resolveFromAddress();

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(to);
      helper.setFrom(from, "MyStash");
      helper.setSubject("Reinitialise ton mot de passe MyStash");
      helper.setText(
          "Tu as demande a reinitialiser ton mot de passe MyStash.\n\n"
              + "Clique sur ce lien pour choisir un nouveau mot de passe :\n"
              + link + "\n\n"
              + "Ce lien expire dans " + expirationMinutes + " minutes.\n"
              + "Si tu n'es pas a l'origine de cette demande, ignore cet email.\n",
          resetPasswordHtml(link)
      );
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

  private String resetPasswordHtml(String link) {
    String safeLink = escapeHtml(link);
    return baseEmailHtml(
        "Modifie ton mot de passe",
        "Pour choisir un nouveau mot de passe MyStash, ouvre ce lien dans ton navigateur.",
        "Choisir un nouveau mot de passe",
        safeLink,
        "Ce lien expire dans " + expirationMinutes + " minutes. Si tu n'as pas demande cette modification, tu peux ignorer cet email.",
        true,
        true
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
