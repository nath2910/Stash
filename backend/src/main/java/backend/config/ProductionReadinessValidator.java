package backend.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionReadinessValidator {

  static final String DEFAULT_DEV_JWT_SECRET = "dev-secret-change-me-dev-secret-change-me";

  private static final Logger log = LoggerFactory.getLogger(ProductionReadinessValidator.class);

  private final Environment environment;
  private final String jwtSecret;
  private final String frontendBaseUrl;
  private final String backendPublicBaseUrl;
  private final String allowedOrigins;

  public ProductionReadinessValidator(
      Environment environment,
      @Value("${app.jwt.secret}") String jwtSecret,
      @Value("${app.frontend.base-url}") String frontendBaseUrl,
      @Value("${app.backend.public-base-url}") String backendPublicBaseUrl,
      @Value("${app.cors.allowed-origins:}") String allowedOrigins
  ) {
    this.environment = environment;
    this.jwtSecret = jwtSecret;
    this.frontendBaseUrl = frontendBaseUrl;
    this.backendPublicBaseUrl = backendPublicBaseUrl;
    this.allowedOrigins = allowedOrigins;
  }

  @PostConstruct
  void validate() {
    boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    if (!prod) {
      if (usesInsecureJwtSecret(jwtSecret)) {
        log.warn("JWT_SECRET is using the default development secret; do not reuse this configuration outside local development");
      }
      return;
    }

    validateJwtSecret(jwtSecret);
    validatePublicHttpsUrl(frontendBaseUrl, "APP_FRONTEND_BASE_URL");
    validatePublicHttpsUrl(backendPublicBaseUrl, "APP_BACKEND_PUBLIC_BASE_URL");
    validateCorsOrigins(allowedOrigins);
  }

  static boolean usesInsecureJwtSecret(String secret) {
    return secret == null
        || secret.isBlank()
        || DEFAULT_DEV_JWT_SECRET.equals(secret)
        || secret.trim().length() < 32;
  }

  static void validateJwtSecret(String secret) {
    if (usesInsecureJwtSecret(secret)) {
      throw new IllegalStateException("JWT_SECRET must be set to a strong secret of at least 32 characters in prod");
    }
  }

  static void validateCorsOrigins(String rawOrigins) {
    if (rawOrigins == null || rawOrigins.isBlank()) {
      throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS must be set in prod");
    }

    for (String rawOrigin : rawOrigins.split(",")) {
      String origin = rawOrigin == null ? "" : rawOrigin.trim();
      if (origin.isEmpty()) {
        continue;
      }
      validateCorsOrigin(origin);
    }
  }

  static void validateCorsOrigin(String rawValue) {
    URI uri = parseAbsoluteUri(rawValue, "APP_CORS_ALLOWED_ORIGINS");
    String scheme = uri.getScheme();
    String host = uri.getHost();
    String normalizedHost = host.toLowerCase(Locale.ROOT);

    if (isLocalHost(normalizedHost)) {
      if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
        throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS localhost entries must use http or https");
      }
      return;
    }

    if (!"https".equalsIgnoreCase(scheme)) {
      throw new IllegalStateException("APP_CORS_ALLOWED_ORIGINS public entries must use https in prod");
    }
  }

  static void validatePublicHttpsUrl(String rawValue, String propertyName) {
    URI uri = parseAbsoluteUri(rawValue, propertyName);
    String scheme = uri.getScheme();
    String host = uri.getHost();
    if (!"https".equalsIgnoreCase(scheme)) {
      throw new IllegalStateException(propertyName + " must use https in prod");
    }

    String normalizedHost = host.toLowerCase(Locale.ROOT);
    if (isLocalHost(normalizedHost)) {
      throw new IllegalStateException(propertyName + " cannot point to localhost in prod");
    }
  }

  private static URI parseAbsoluteUri(String rawValue, String propertyName) {
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
    return uri;
  }

  private static boolean isLocalHost(String normalizedHost) {
    return normalizedHost.equals("localhost")
        || normalizedHost.equals("127.0.0.1")
        || normalizedHost.equals("0.0.0.0")
        || normalizedHost.endsWith(".localhost");
  }
}
