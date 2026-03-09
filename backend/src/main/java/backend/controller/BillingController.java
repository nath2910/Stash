package backend.controller;

import backend.dto.BillingStatusResponse;
import backend.dto.CheckoutRequest;
import backend.dto.CheckoutResponse;
import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.BillingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/billing")
public class BillingController {

  private final BillingService billingService;
  private final UserRepository userRepository;

  public BillingController(BillingService billingService, UserRepository userRepository) {
    this.billingService = billingService;
    this.userRepository = userRepository;
  }

  @GetMapping("/status")
  public BillingStatusResponse status(@AuthenticationPrincipal User user) {
    if (!billingService.isConfigured()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe non configuré");
    }
    try {
      billingService.refreshStatus(user);
      var portal = billingService.createPortal(user);
      return new BillingStatusResponse(user.getSubscriptionStatus(), portal.getUrl());
    } catch (Exception e) {
      return new BillingStatusResponse(user.getSubscriptionStatus(), "");
    }
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
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
    }
  }

  @PostMapping("/webhook")
  public ResponseEntity<Void> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sig) {
    billingService.handleWebhook(payload, sig);
    return ResponseEntity.ok().build();
  }
}
