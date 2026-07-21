package backend.service;

import backend.dto.TrackingConnectResponse;
import backend.entity.MailAccount;
import backend.entity.MailAccountStatus;
import backend.entity.MailProvider;
import backend.entity.User;
import backend.repository.MailAccountRepository;
import backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class GmailOAuthService {

  public static final String GMAIL_READONLY_SCOPE = "https://www.googleapis.com/auth/gmail.readonly";

  private static final URI GOOGLE_AUTH_URI = URI.create("https://accounts.google.com/o/oauth2/v2/auth");
  private static final URI GOOGLE_TOKEN_URI = URI.create("https://oauth2.googleapis.com/token");
  private static final URI GMAIL_PROFILE_URI = URI.create("https://gmail.googleapis.com/gmail/v1/users/me/profile");

  private final String googleClientId;
  private final String googleClientSecret;
  private final String gmailRedirectUri;
  private final String jwtSecret;
  private final String frontendBaseUrl;
  private final String oauthSuccessRedirect;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final Environment environment;
  private final UserRepository userRepository;
  private final MailAccountRepository mailAccountRepository;
  private final TokenEncryptionService tokenEncryptionService;

  public GmailOAuthService(
      @Value("${spring.security.oauth2.client.registration.google.client-id:}") String googleClientId,
      @Value("${spring.security.oauth2.client.registration.google.client-secret:}") String googleClientSecret,
      @Value("${app.delivery.gmail-redirect-uri:http://localhost:8080/delivery/mail-accounts/gmail/callback}") String gmailRedirectUri,
      @Value("${app.jwt.secret:dev-secret-change-me-dev-secret-change-me}") String jwtSecret,
      @Value("${app.frontend.base-url:http://localhost:5173}") String frontendBaseUrl,
      @Value("${app.oauth2.success-redirect:http://localhost:5173/auth/callback}") String oauthSuccessRedirect,
      ObjectMapper objectMapper,
      Environment environment,
      UserRepository userRepository,
      MailAccountRepository mailAccountRepository,
      TokenEncryptionService tokenEncryptionService
  ) {
    this.googleClientId = googleClientId;
    this.googleClientSecret = googleClientSecret;
    this.gmailRedirectUri = gmailRedirectUri;
    this.jwtSecret = jwtSecret;
    this.frontendBaseUrl = frontendBaseUrl;
    this.oauthSuccessRedirect = oauthSuccessRedirect;
    this.restClient = RestClient.builder().build();
    this.objectMapper = objectMapper;
    this.environment = environment;
    this.userRepository = userRepository;
    this.mailAccountRepository = mailAccountRepository;
    this.tokenEncryptionService = tokenEncryptionService;
  }

  @PostConstruct
  void validateProductionConfig() {
    boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    if (!prod) {
      return;
    }

    boolean hasGoogleClientId = googleClientId != null && !googleClientId.isBlank();
    boolean hasGoogleClientSecret = googleClientSecret != null && !googleClientSecret.isBlank();
    boolean gmailOauthConfigured = hasGoogleClientId || hasGoogleClientSecret;

    if (gmailOauthConfigured && (!hasGoogleClientId || !hasGoogleClientSecret)) {
      throw new IllegalStateException("GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET must both be set in prod");
    }
    if (!gmailOauthConfigured) {
      return;
    }

    requirePublicHttpsUrl(gmailRedirectUri, "APP_DELIVERY_GMAIL_REDIRECT_URI");
    requirePublicHttpsUrl(frontendBaseUrl, "APP_FRONTEND_BASE_URL");
    requirePublicHttpsUrl(oauthSuccessRedirect, "OAUTH2_SUCCESS_REDIRECT");
  }

  public TrackingConnectResponse buildConnectResponse(User user, String emailHint) {
    if (user == null || user.getId() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
    }
    if (googleClientId == null || googleClientId.isBlank() || googleClientSecret == null || googleClientSecret.isBlank()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OAuth Gmail non configure");
    }

    String state = createState(user.getId());
    UriComponentsBuilder builder = UriComponentsBuilder.fromUri(GOOGLE_AUTH_URI)
        .queryParam("client_id", googleClientId)
        .queryParam("redirect_uri", gmailRedirectUri)
        .queryParam("response_type", "code")
        .queryParam("scope", GMAIL_READONLY_SCOPE)
        .queryParam("access_type", "offline")
        .queryParam("prompt", "consent select_account")
        .queryParam("include_granted_scopes", "false")
        .queryParam("state", state);

    String loginHint = normalizeEmailHint(emailHint);
    if (loginHint != null) {
      builder.queryParam("login_hint", loginHint);
    }

    String authorizationUrl = builder
        .build()
        .toUriString();

    return new TrackingConnectResponse(authorizationUrl);
  }

  public MailAccount handleCallback(String code, String state) {
    if (code == null || code.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code OAuth manquant");
    }

    Long userId = verifyStateAndExtractUserId(state);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

    Map<String, Object> tokenResponse = exchangeCode(code);
    String accessToken = stringValue(tokenResponse.get("access_token"));
    String refreshToken = stringValue(tokenResponse.get("refresh_token"));
    if (accessToken == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Token Gmail absent");
    }

    Map<String, Object> profile = fetchGmailProfile(accessToken);
    String emailAddress = stringValue(profile.get("emailAddress"));
    String providerAccountId = stringValue(profile.get("emailAddress"));
    if (emailAddress == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Profil Gmail incomplet");
    }

    String normalizedEmail = emailAddress.trim().toLowerCase();
    MailAccount account = mailAccountRepository.findByProviderAndProviderAccountId(MailProvider.GMAIL, providerAccountId)
        .or(() -> mailAccountRepository.findByUser_IdAndProviderAndEmailAddress(user.getId(), MailProvider.GMAIL, normalizedEmail))
        .orElseGet(MailAccount::new);

    if (account.getId() != null && account.getUser() != null && !user.getId().equals(account.getUser().getId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Compte Gmail deja lie a un autre utilisateur");
    }
    if ((refreshToken == null || refreshToken.isBlank())
        && (account.getEncryptedRefreshToken() == null || account.getEncryptedRefreshToken().isBlank())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refresh token Gmail absent; reessayez la liaison Gmail");
    }

    account.setUser(user);
    account.setProvider(MailProvider.GMAIL);
    account.setProviderAccountId(providerAccountId);
    account.setEmailAddress(normalizedEmail);
    account.setScopes(firstNonBlank(stringValue(tokenResponse.get("scope")), GMAIL_READONLY_SCOPE));
    String historyId = stringValue(profile.get("historyId"));
    if (historyId != null) {
      account.setScanCursor(historyId);
    }
    if (refreshToken != null && !refreshToken.isBlank()) {
      account.setEncryptedRefreshToken(tokenEncryptionService.encrypt(refreshToken));
    }
    account.setEncryptedAccessToken(tokenEncryptionService.encrypt(accessToken));
    account.setAccessTokenExpiresAt(expiresAt(tokenResponse.get("expires_in")));
    account.setStatus(MailAccountStatus.ACTIVE);
    account.setErrorCount(0);
    account.setNextScanAt(OffsetDateTime.now(ZoneOffset.UTC));
    MailAccount saved = mailAccountRepository.save(account);
    removeSecondaryAccounts(user.getId(), saved.getId());
    return saved;
  }

  public URI buildFrontendRedirect(boolean success) {
    String base = frontendOrigin();
    String marker = success ? "connected" : "error";
    return URI.create(base + "/gestion?tab=delivery&gmail=" + marker);
  }

  private Map<String, Object> exchangeCode(String code) {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", code);
    form.add("client_id", googleClientId);
    form.add("client_secret", googleClientSecret);
    form.add("redirect_uri", gmailRedirectUri);
    form.add("grant_type", "authorization_code");

    try {
      return restClient.post()
          .uri(GOOGLE_TOKEN_URI)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(form)
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Echange OAuth Gmail impossible", ex);
    }
  }

  private Map<String, Object> fetchGmailProfile(String accessToken) {
    try {
      return restClient.get()
          .uri(GMAIL_PROFILE_URI)
          .headers(headers -> headers.setBearerAuth(accessToken))
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Profil Gmail inaccessible", ex);
    }
  }

  private String createState(Long userId) {
    try {
      Map<String, Object> payload = Map.of(
          "userId", userId,
          "nonce", UUID.randomUUID().toString(),
          "exp", OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10).toEpochSecond()
      );
      String encodedPayload = base64Url(objectMapper.writeValueAsBytes(payload));
      String signature = base64Url(hmac(encodedPayload.getBytes(StandardCharsets.UTF_8)));
      return encodedPayload + "." + signature;
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to create Gmail OAuth state", ex);
    }
  }

  private Long verifyStateAndExtractUserId(String state) {
    if (state == null || state.isBlank() || !state.contains(".")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State OAuth invalide");
    }
    String[] parts = state.split("\\.", 2);
    String expected = base64Url(hmac(parts[0].getBytes(StandardCharsets.UTF_8)));
    if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State OAuth invalide");
    }

    try {
      JsonNode node = objectMapper.readTree(Base64.getUrlDecoder().decode(padBase64(parts[0])));
      long exp = node.path("exp").asLong(0);
      if (OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond() > exp) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State OAuth expire");
      }
      long userId = node.path("userId").asLong(0);
      if (userId <= 0) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State OAuth invalide");
      }
      return userId;
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State OAuth invalide", ex);
    }
  }

  private OffsetDateTime expiresAt(Object expiresInValue) {
    long expiresIn = 3600L;
    if (expiresInValue instanceof Number number) {
      expiresIn = number.longValue();
    } else if (expiresInValue != null) {
      try {
        expiresIn = Long.parseLong(String.valueOf(expiresInValue));
      } catch (NumberFormatException ignored) {
        expiresIn = 3600L;
      }
    }
    return OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(Math.max(60, expiresIn - 60));
  }

  private void removeSecondaryAccounts(Long userId, Long keepAccountId) {
    if (userId == null || keepAccountId == null) {
      return;
    }
    List<MailAccount> secondaryAccounts = mailAccountRepository.findByUser_IdAndIdNotOrderByCreatedAtDesc(
        userId,
        keepAccountId
    );
    if (!secondaryAccounts.isEmpty()) {
      mailAccountRepository.deleteAll(secondaryAccounts);
    }
  }

  private byte[] hmac(byte[] value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return mac.doFinal(value);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to sign Gmail OAuth state", ex);
    }
  }

  private String frontendOrigin() {
    String configuredOrigin = absoluteOrigin(frontendBaseUrl);
    if (configuredOrigin != null) {
      return configuredOrigin;
    }

    String successRedirectOrigin = absoluteOrigin(oauthSuccessRedirect);
    if (successRedirectOrigin != null) {
      return successRedirectOrigin;
    }

    String derivedFromApi = deriveFrontendOriginFromApi(gmailRedirectUri);
    if (derivedFromApi != null) {
      return derivedFromApi;
    }

    return "http://localhost:5173";
  }

  private String absoluteOrigin(String rawValue) {
    try {
      URI uri = URI.create(rawValue);
      if (uri.getScheme() != null && uri.getAuthority() != null) {
        return uri.getScheme() + "://" + uri.getAuthority();
      }
    } catch (Exception ignored) {
      // fallback below
    }
    return null;
  }

  private void requirePublicHttpsUrl(String rawValue, String propertyName) {
    URI uri;
    try {
      uri = URI.create(rawValue);
    } catch (Exception ex) {
      throw new IllegalStateException(propertyName + " must be a valid absolute URL in prod", ex);
    }

    String scheme = uri.getScheme();
    String host = uri.getHost();
    if (scheme == null || host == null) {
      throw new IllegalStateException(propertyName + " must be a valid absolute URL in prod");
    }
    if (!"https".equalsIgnoreCase(scheme)) {
      throw new IllegalStateException(propertyName + " must use https in prod");
    }
    if (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1")) {
      throw new IllegalStateException(propertyName + " cannot point to localhost in prod");
    }
  }

  private String deriveFrontendOriginFromApi(String apiUrl) {
    try {
      URI uri = URI.create(apiUrl);
      if (uri.getScheme() == null || uri.getHost() == null) {
        return null;
      }

      String host = uri.getHost();
      if (host.equalsIgnoreCase("localhost") || host.equals("127.0.0.1")) {
        return "http://localhost:5173";
      }

      String frontendHost = host;
      if (host.startsWith("api.")) {
        frontendHost = host.substring(4);
      } else if (host.startsWith("api-")) {
        frontendHost = host.substring(4);
      }

      int port = uri.getPort();
      String authority = port > 0 && port != 80 && port != 443 ? frontendHost + ":" + port : frontendHost;
      return uri.getScheme() + "://" + authority;
    } catch (Exception ignored) {
      return null;
    }
  }

  private String base64Url(byte[] bytes) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String padBase64(String value) {
    int remainder = value.length() % 4;
    return remainder == 0 ? value : value + "=".repeat(4 - remainder);
  }

  private String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  private String firstNonBlank(String first, String fallback) {
    return first == null || first.isBlank() ? fallback : first;
  }

  private String normalizeEmailHint(String emailHint) {
    if (emailHint == null || emailHint.isBlank()) {
      return null;
    }
    String normalized = emailHint.trim().toLowerCase(Locale.ROOT);
    return normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") ? normalized : null;
  }
}
