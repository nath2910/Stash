package backend.controller;

import backend.dto.BillingStatusResponse;
import backend.dto.CheckoutRequest;
import backend.dto.CheckoutResponse;
import backend.entity.User;
import backend.service.BillingService;
import backend.service.BillingService.ValidatedPromo;
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
      if (forceRefresh || user.getStripeCustomerId() != null) {
        billingService.refreshStatus(user);
      }
      String portalUrl = "";
      if (includePortal && isPortalEligibleStatus(user.getSubscriptionStatus())) {
        var portal = billingService.createPortal(user);
        portalUrl = portal.getUrl();
      }
      return snapshot(user, portalUrl);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Facturation temporairement indisponible");
    }
  }

  public BillingStatusResponse status(User user, boolean includePortal) {
    return status(user, includePortal, true);
  }

  @PostMapping("/checkout")
  public CheckoutResponse checkout(@AuthenticationPrincipal User user, @RequestBody @jakarta.validation.Valid CheckoutRequest request) {
    if (!billingService.isConfigured()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe non configuré");
    }
    try {
      var session = billingService.createCheckout(user, request);
      return new CheckoutResponse(session.getUrl());
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception e) {
      log.warn("Billing checkout failed");
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Session de paiement indisponible", e);
    }
  }

  @PostMapping("/webhook")
  public ResponseEntity<Void> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sig) {
    billingService.handleWebhook(payload, sig);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/plans")
  public java.util.List<BillingService.Plan> plans() throws Exception { return billingService.plans(); }

  @PostMapping("/cancel")
  public BillingStatusResponse cancel(@AuthenticationPrincipal User user) throws Exception {
    billingService.cancelAtPeriodEnd(user);
    return snapshot(user, "");
  }

  @PostMapping("/portal")
  public CheckoutResponse portal(@AuthenticationPrincipal User user) throws Exception {
    return new CheckoutResponse(billingService.createPortal(user).getUrl());
  }

  @PostMapping("/validate-promo")
  public ValidatedPromo validatePromo(@RequestParam String code) {
    if (code == null || code.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code requis");
    }
    try {
      return billingService.validatePromo(code.strip());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Validation du code promo impossible");
    }
  }

  private BillingStatusResponse snapshot(User user, String portalUrl) {
    return new BillingStatusResponse(user.getSubscriptionStatus(), portalUrl,
        user.getSubscriptionCurrentPeriodEnd(), user.isSubscriptionCancelAtPeriodEnd());
  }

  private boolean isPortalEligibleStatus(String status) {
    if (status == null) return false;
    return !"inactive".equalsIgnoreCase(status);
  }
}
