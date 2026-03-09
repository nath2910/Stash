package backend.security;

import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.DiscordAccessService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final DiscordAccessService discordAccessService;

  @Value("${app.oauth2.success-redirect}")
  private String successRedirect;

  public OAuth2SuccessHandler(UserRepository userRepository, JwtService jwtService, DiscordAccessService discordAccessService) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.discordAccessService = discordAccessService;
  }

  private static String norm(String s) { return s == null ? "" : s.trim(); }
  private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
    Object principal = authentication.getPrincipal();

    if (principal instanceof OidcUser oidcUser) {
      handleGoogle(oidcUser, response);
      return;
    }

    if (principal instanceof OAuth2User oauthUser) {
      handleDiscord(oauthUser, response);
      return;
    }

    response.sendError(400, "Unsupported OAuth principal type");
  }

  private void handleGoogle(OidcUser oidcUser, HttpServletResponse response) throws IOException {
    String sub = oidcUser.getSubject();
    String email = oidcUser.getEmail();
    String givenName = oidcUser.getGivenName();
    String familyName = oidcUser.getFamilyName();
    String picture = oidcUser.getPicture();
    Boolean emailVerified = oidcUser.getEmailVerified();

    if (sub == null || email == null) {
      response.sendError(400, "Google login missing sub/email");
      return;
    }

    String emailNorm = email.toLowerCase().trim();

    Optional<User> byGoogle = userRepository.findByProviderAndProviderId("GOOGLE", sub);

    User user = byGoogle.orElseGet(() -> {
      Optional<User> byEmail = userRepository.findByEmail(emailNorm);
      if (byEmail.isPresent()) {
        User u = byEmail.get();
        u.setProvider("GOOGLE");
        u.setProviderId(sub);
        u.setEmailVerified(Boolean.TRUE.equals(emailVerified));
        if (!isBlank(givenName)) u.setFirstName(norm(givenName));
        if (!isBlank(familyName)) u.setLastName(norm(familyName));
        if (!isBlank(picture)) u.setPictureUrl(norm(picture));
        u.setEmail(emailNorm);
        return userRepository.save(u);
      }
      User u = new User();
      u.setEmail(emailNorm);
      u.setFirstName(!isBlank(givenName) ? norm(givenName) : "");
      u.setLastName(!isBlank(familyName) ? norm(familyName) : "");
      u.setProvider("GOOGLE");
      u.setProviderId(sub);
      u.setEmailVerified(Boolean.TRUE.equals(emailVerified));
      if (!isBlank(picture)) u.setPictureUrl(norm(picture));
      u.setPassword(null);
      return userRepository.save(u);
    });

    boolean changed = false;
    if (!emailNorm.equals(user.getEmail())) { user.setEmail(emailNorm); changed = true; }
    if (!isBlank(givenName) && !norm(givenName).equals(user.getFirstName())) { user.setFirstName(norm(givenName)); changed = true; }
    if (!isBlank(familyName) && !norm(familyName).equals(user.getLastName())) { user.setLastName(norm(familyName)); changed = true; }
    if (!isBlank(picture) && (user.getPictureUrl() == null || !norm(picture).equals(user.getPictureUrl()))) { user.setPictureUrl(norm(picture)); changed = true; }
    user.setEmailVerified(Boolean.TRUE.equals(emailVerified));
    if (changed) user = userRepository.save(user);

    String jwt = jwtService.generateToken(user.getId());
    String token = URLEncoder.encode(jwt, StandardCharsets.UTF_8);
    response.sendRedirect(successRedirect + "#token=" + token);
  }

  private void handleDiscord(OAuth2User oauthUser, HttpServletResponse response) throws IOException {
    String discordId = String.valueOf(oauthUser.getAttributes().get("id"));
    String email = (String) oauthUser.getAttributes().get("email");
    Boolean emailVerified = (Boolean) oauthUser.getAttributes().get("verified");
    String globalName = (String) oauthUser.getAttributes().get("global_name");
    String username = !isBlank(globalName) ? globalName : (String) oauthUser.getAttributes().get("username");

    if (isBlank(discordId)) {
      response.sendError(400, "Discord login missing id");
      return;
    }

    String emailNorm = !isBlank(email) ? email.trim().toLowerCase() : null;

    // Vérifie l'éligibilité AVANT toute persistance
    User probe = new User();
    probe.setDiscordId(discordId);
    boolean eligible = discordAccessService.isEligible(probe);
    if (!eligible) {
      response.sendRedirect(successRedirect + "#error=discord_not_allowed");
      return;
    }

    Optional<User> byDiscord = userRepository.findByProviderAndProviderId("DISCORD", discordId);

    User user = byDiscord.orElseGet(() -> {
      if (!isBlank(emailNorm)) {
        Optional<User> byEmail = userRepository.findByEmail(emailNorm);
        if (byEmail.isPresent()) {
          User u = byEmail.get();
          u.setProvider("DISCORD");
          u.setProviderId(discordId);
          u.setDiscordId(discordId);
          if (!isBlank(username)) u.setFirstName(norm(username));
          if (emailVerified != null) u.setEmailVerified(emailVerified);
          return userRepository.save(u);
        }
      }
      User u = new User();
      u.setProvider("DISCORD");
      u.setProviderId(discordId);
      u.setDiscordId(discordId);
      u.setFirstName(!isBlank(username) ? norm(username) : "");
      u.setLastName("");
      String safeEmail = !isBlank(emailNorm) ? emailNorm : ("discord_" + discordId + "@discord.local");
      u.setEmail(safeEmail);
      u.setEmailVerified(Boolean.TRUE.equals(emailVerified));
      u.setPassword(""); // OAuth users n'ont pas de mot de passe local
      return userRepository.save(u);
    });

    boolean changed = false;
    if (user.getDiscordId() == null || !discordId.equals(user.getDiscordId())) { user.setDiscordId(discordId); changed = true; }
    if (!isBlank(emailNorm) && !emailNorm.equals(user.getEmail())) { user.setEmail(emailNorm); changed = true; }
    if (!isBlank(username) && !norm(username).equals(user.getFirstName())) { user.setFirstName(norm(username)); changed = true; }
    if (emailVerified != null && emailVerified != user.isEmailVerified()) { user.setEmailVerified(emailVerified); changed = true; }
    // Si password est null (anciennes données), on met une chaîne vide pour respecter NOT NULL.
    if (user.getPassword() == null) { user.setPassword(""); changed = true; }

    if (changed) user = userRepository.save(user);

    if (!"active".equals(user.getSubscriptionStatus())) {
      user.setSubscriptionStatus("active");
      userRepository.save(user);
    }

    String jwt = jwtService.generateToken(user.getId());
    String token = URLEncoder.encode(jwt, StandardCharsets.UTF_8);
    response.sendRedirect(successRedirect + "#token=" + token);
  }
}
