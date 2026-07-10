package backend.controller;

import backend.dto.GmailConnectRequest;
import backend.dto.MailAccountResponse;
import backend.dto.MailScanBatchResponse;
import backend.dto.MailScanResponse;
import backend.dto.TrackingConnectResponse;
import backend.entity.User;
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

  public MailAccountController(MailAccountService mailAccountService) {
    this.mailAccountService = mailAccountService;
  }

  @GetMapping
  public List<MailAccountResponse> list(@AuthenticationPrincipal User currentUser) {
    return mailAccountService.listForUser(currentUser.getId());
  }

  @PostMapping("/gmail/connect")
  public TrackingConnectResponse connectGmail(
      @AuthenticationPrincipal User currentUser,
      @RequestBody(required = false) GmailConnectRequest request
  ) {
    String emailHint = request != null ? request.emailAddress() : null;
    return mailAccountService.connectGmail(currentUser, emailHint);
  }

  @GetMapping("/gmail/callback")
  public ResponseEntity<Void> gmailCallback(
      @RequestParam(name = "code", required = false) String code,
      @RequestParam(name = "state", required = false) String state
  ) {
    boolean success = false;
    try {
      mailAccountService.completeGmailCallback(code, state);
      success = true;
    } catch (Exception ex) {
      log.warn(
          "Gmail delivery OAuth callback failed: {} - {}",
          ex.getClass().getSimpleName(),
          ex.getMessage(),
          ex
      );
    }

    URI redirect = mailAccountService.gmailCallbackRedirect(success);
    return ResponseEntity.status(HttpStatus.FOUND)
        .header(HttpHeaders.LOCATION, redirect.toString())
        .build();
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
    mailAccountService.deleteForUser(currentUser.getId(), id);
  }

  @PostMapping("/{id}/scan-now")
  public MailScanResponse scanNow(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
    return mailAccountService.scanNow(currentUser.getId(), id);
  }

  @PostMapping("/scan-all")
  public MailScanBatchResponse scanAll(@AuthenticationPrincipal User currentUser) {
    return mailAccountService.scanAll(currentUser.getId());
  }
}
