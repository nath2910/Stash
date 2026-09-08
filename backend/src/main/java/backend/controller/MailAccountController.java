package backend.controller;

import backend.dto.GmailConnectRequest;
import backend.dto.MailAccountResponse;
import backend.dto.MailScanBatchResponse;
import backend.dto.MailScanResponse;
import backend.dto.TrackingConnectResponse;
import backend.entity.User;
import backend.security.RequiresActiveSubscription;
import backend.service.MailAccountService;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/delivery/mail-accounts")
public class MailAccountController {

  private static final Logger log = LoggerFactory.getLogger(MailAccountController.class);

  private final MailAccountService mailAccountService;
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;

  public MailAccountController(MailAccountService mailAccountService, org.springframework.jdbc.core.JdbcTemplate jdbc) {
    this.mailAccountService = mailAccountService;
    this.jdbc = jdbc;
  }

  @GetMapping
  @RequiresActiveSubscription
  public List<MailAccountResponse> list(@AuthenticationPrincipal User currentUser) {
    return mailAccountService.listForUser(currentUser.getId());
  }

  @PostMapping("/gmail/connect")
  @RequiresActiveSubscription
  public TrackingConnectResponse connectGmail(
      @AuthenticationPrincipal User currentUser,
      @RequestBody(required = false) GmailConnectRequest request,
      jakarta.servlet.http.HttpServletRequest servletRequest,
      jakarta.servlet.http.HttpServletResponse response
  ) {
    String emailHint = request != null ? request.emailAddress() : null;
    var result = mailAccountService.connectGmail(currentUser, emailHint);
    String state = org.springframework.web.util.UriComponentsBuilder.fromUriString(result.authorizationUrl())
        .build().getQueryParams().getFirst("state");
    String hash = backend.security.SensitiveTokenHasher.hash(state);
    jdbc.update("INSERT INTO gmail_oauth_states(state_hash, user_id, expires_at) VALUES (?, ?, now() + interval '10 minutes')", hash, currentUser.getId());
    response.addHeader(HttpHeaders.SET_COOKIE, org.springframework.http.ResponseCookie.from("gmail_link_state", hash)
        .httpOnly(true).secure(servletRequest.isSecure()).sameSite("Lax").path("/delivery/mail-accounts/gmail")
        .maxAge(600).build().toString());
    return result;
  }

  @GetMapping("/gmail/callback")
  public ResponseEntity<Void> gmailCallback(
      @RequestParam(name = "code", required = false) String code,
      @RequestParam(name = "state", required = false) String state,
      @org.springframework.web.bind.annotation.CookieValue(name = "gmail_link_state", required = false) String cookie,
      jakarta.servlet.http.HttpServletRequest request
  ) {
    boolean success = false;
    try {
      String hash = state == null ? "" : backend.security.SensitiveTokenHasher.hash(state);
      if (cookie == null || !java.security.MessageDigest.isEqual(hash.getBytes(java.nio.charset.StandardCharsets.UTF_8),
          cookie.getBytes(java.nio.charset.StandardCharsets.UTF_8))
          || jdbc.update("DELETE FROM gmail_oauth_states WHERE state_hash = ? AND expires_at > now()", hash) != 1) {
        throw new IllegalArgumentException("Invalid OAuth state");
      }
      mailAccountService.completeGmailCallback(code, state);
      success = true;
    } catch (Exception ex) {
      log.warn("Gmail delivery OAuth callback failed");
    }

    URI redirect = mailAccountService.gmailCallbackRedirect(success);
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.SET_COOKIE, org.springframework.http.ResponseCookie.from("gmail_link_state", "")
            .httpOnly(true).secure(request.isSecure()).sameSite("Lax").path("/delivery/mail-accounts/gmail").maxAge(0).build().toString())
        .header(HttpHeaders.LOCATION, redirect.toString())
        .build();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
    mailAccountService.deleteForUser(currentUser.getId(), id);
  }

  @PostMapping("/{id}/scan-now")
  @RequiresActiveSubscription
  public MailScanResponse scanNow(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
    return mailAccountService.scanNow(currentUser.getId(), id);
  }

  @PostMapping("/scan-all")
  @RequiresActiveSubscription
  public MailScanBatchResponse scanAll(@AuthenticationPrincipal User currentUser) {
    return mailAccountService.scanAll(currentUser.getId());
  }
}
