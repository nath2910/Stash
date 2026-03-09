package backend.security;

import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.client.HttpStatusCodeException;

/**
 * Wraps the default token response client and retries once when Discord returns HTTP 429.
 * Simple and non-invasive: respects Retry-After header if present, otherwise waits 1s.
 */
public class RetryingTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

  private final DefaultAuthorizationCodeTokenResponseClient delegate = new DefaultAuthorizationCodeTokenResponseClient();

  @Override
  public org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse getTokenResponse(
      OAuth2AuthorizationCodeGrantRequest authorizationGrantRequest) {
    try {
      return delegate.getTokenResponse(authorizationGrantRequest);
    } catch (OAuth2AuthorizationException ex) {
      if (isRateLimited(ex)) {
        sleep(retryAfterMillis(ex));
        return delegate.getTokenResponse(authorizationGrantRequest);
      }
      throw ex;
    }
  }

  private static boolean isRateLimited(OAuth2AuthorizationException ex) {
    Throwable cause = ex.getCause();
    if (cause instanceof HttpStatusCodeException http) {
      return http.getStatusCode().value() == 429;
    }
    OAuth2Error error = ex.getError();
    return error != null && "server_error".equals(error.getErrorCode());
  }

  private static long retryAfterMillis(OAuth2AuthorizationException ex) {
    Throwable cause = ex.getCause();
    if (cause instanceof HttpStatusCodeException http) {
      String ra = http.getResponseHeaders() != null ? http.getResponseHeaders().getFirst("Retry-After") : null;
      if (ra != null) {
        try {
          return Math.max(500, Long.parseLong(ra) * 1000);
        } catch (NumberFormatException ignored) {}
      }
    }
    return 1000L; // default 1s
  }

  private static void sleep(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }
}
