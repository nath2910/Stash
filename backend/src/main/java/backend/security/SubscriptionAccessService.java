package backend.security;

import backend.entity.User;
import backend.service.DiscordAccessService;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SubscriptionAccessService {

  private final DiscordAccessService discordAccessService;

  public SubscriptionAccessService(DiscordAccessService discordAccessService) {
    this.discordAccessService = discordAccessService;
  }

  public boolean hasActiveSubscription(User user) {
    if (user == null) return false;
    if (hasUsableStripeAccess(user)) {
      return true;
    }
    // Bypass: Discord-eligible users get access even without Stripe subscription.
    return isDiscordEligible(user);
  }

  public void requireActiveSubscription(User user) {
    if (user == null || user.getId() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
    }
    if (!hasActiveSubscription(user)) {
      throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Abonnement actif requis");
    }
  }

  boolean isDiscordEligible(User user) {
    try {
      return discordAccessService.isEligible(user);
    } catch (Exception e) {
      return false;
    }
  }

  static boolean isActiveStatus(String status) {
    return "active".equalsIgnoreCase(String.valueOf(status).trim())
        || "trialing".equalsIgnoreCase(String.valueOf(status).trim());
  }

  private boolean hasUsableStripeAccess(User user) {
    if (!isActiveStatus(user.getSubscriptionStatus())) {
      return false;
    }
    OffsetDateTime periodEnd = user.getSubscriptionCurrentPeriodEnd();
    return periodEnd == null || periodEnd.isAfter(OffsetDateTime.now());
  }
}
