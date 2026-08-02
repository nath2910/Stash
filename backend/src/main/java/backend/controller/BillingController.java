package backend.controller;

import backend.dto.BillingStatusResponse;
import backend.dto.CheckoutRequest;
import backend.dto.CheckoutResponse;
import backend.entity.User;
import backend.service.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/billing")
public class BillingController {

  private static final Logger log = LoggerFactory.getLogger(BillingController.class);

  private final BillingService billingService;

  public BillingController(BillingService billingService) {
    this.billingService = billingService;
  }

  @GetMapping("/status")
  public BillingStatusResponse status(
      @AuthenticationPrincipal User user,
      @RequestParam(name = "includePortal", defaultValue = "false") boolean includePortal,
      @RequestParam(name = "forceRefresh", defaultValue = "false") boolean forceRefresh
  ) {
    if (!billingService.isConfigured()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe non configuré");
    }
    try {
      if (forceRefresh) {
        billingService.refreshStatus(user);
      }
      String portalUrl = "";
      if (includePortal && isPortalEligibleStatus(user.getSubscriptionStatus())) {
        var portal = billingService.createPortal(user);
        portalUrl = portal.getUrl();
      }
      return new BillingStatusResponse(user.getSubscriptionStatus(), portalUrl);
    } catch (Exception e) {
      log.warn("Billing status refresh failed for user {}", user != null ? user.getId() : null, e);
      return new BillingStatusResponse(user.getSubscriptionStatus(), "");
    }
  }

  public BillingStatusResponse status(User user, boolean includePortal) {
    return status(user, includePortal, true);
  }

  @PostMapping("/checkout")
  public CheckoutResponse checkout(@AuthenticationPrincipal User user, @RequestBody CheckoutRequest request) {
    if (!billingService.isConfigured()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe non configuré");
    }
    try {
      String promo = request != null ? request.promoCode() : null;
      String discord = request != null ? request.discord() : null;
      var session = billingService.createCheckout(user, promo, discord);
      return new CheckoutResponse(session.getUrl());
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception e) {
      log.warn("Billing checkout failed for user {}", user != null ? user.getId() : null, e);
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Session de paiement indisponible", e);
    }
  }

  @PostMapping("/webhook")
  public ResponseEntity<Void> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sig) {
    billingService.handleWebhook(payload, sig);
    return ResponseEntity.ok().build();
  }

  private boolean isPortalEligibleStatus(String status) {
    if (status == null) return false;
    return "active".equalsIgnoreCase(status)
        || "past_due".equalsIgnoreCase(status)
        || "canceled".equalsIgnoreCase(status);
  }
}
