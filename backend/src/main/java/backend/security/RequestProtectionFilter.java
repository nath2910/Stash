package backend.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded, process-local abuse protection; production ingress must also limit traffic. */
public class RequestProtectionFilter extends OncePerRequestFilter {
  private static final int MAX_BODY = 2 * 1024 * 1024;
  private final Cache<String, Window> windows = Caffeine.newBuilder()
      .maximumSize(20_000).expireAfterWrite(Duration.ofMinutes(1)).build();

  static class Window {
    private int count;
    synchronized boolean allow(int limit) { return ++count <= limit; }
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    String path = request.getServletPath();
    if (path.isEmpty()) path = request.getRequestURI();
    if (path.equals("/health") || path.equals("/ping") || request.getMethod().equals("OPTIONS")) {
      chain.doFilter(request, response);
      return;
    }
    boolean webhook = path.equals("/billing/webhook") || path.equals("/delivery/webhooks/aftership");
    String category = path.startsWith("/auth/") && !path.equals("/auth/me") ? "auth"
        : path.startsWith("/billing/") ? "billing"
        : path.startsWith("/delivery/") && !request.getMethod().equals("GET") ? "delivery" : "api";
    int limit = category.equals("auth") ? 20 : category.equals("billing") || category.equals("delivery") ? 30 : 300;
    // Do not read an arbitrary X-Forwarded-For header here. Trust is configured at the ingress.
    if (!webhook && !windows.get(category + ":" + request.getRemoteAddr(), k -> new Window()).allow(limit)) {
      response.setHeader("Retry-After", "60");
      reject(response, 429, "Trop de requêtes. Réessayez dans une minute.");
      return;
    }
    if (request.getRequestURI().length() > 2048
        || (request.getQueryString() != null && request.getQueryString().length() > 4096)) {
      reject(response, 414, "URL trop longue");
      return;
    }
    String contentType = request.getContentType();
    if (contentType != null && (contentType.startsWith("application/json") || contentType.startsWith("text/"))) {
      if (request.getContentLengthLong() > MAX_BODY) {
        reject(response, 413, "Requête trop volumineuse");
        return;
      }
      byte[] body = request.getInputStream().readNBytes(MAX_BODY + 1);
      if (body.length > MAX_BODY) {
        reject(response, 413, "Requête trop volumineuse");
        return;
      }
      chain.doFilter(new HttpServletRequestWrapper(request) {
        @Override public ServletInputStream getInputStream() {
          ByteArrayInputStream input = new ByteArrayInputStream(body);
          return new ServletInputStream() {
            @Override public int read() { return input.read(); }
            @Override public boolean isFinished() { return input.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) { throw new UnsupportedOperationException(); }
          };
        }
      }, response);
      return;
    }
    chain.doFilter(request, response);
  }

  private void reject(HttpServletResponse response, int status, String message) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    response.getWriter().write("{\"message\":\"" + message + "\"}");
  }
}
