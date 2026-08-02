package backend.security;

import backend.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SubscriptionAccessService {

  public boolean hasActiveSubscription(User user) {
    return user != null && isActiveStatus(user.getSubscriptionStatus());
  }

  public void requireActiveSubscription(User user) {
    if (user == null || user.getId() == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentification requise");
    }
    if (!hasActiveSubscription(user)) {
      throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Abonnement actif requis");
    }
  }

  static boolean isActiveStatus(String status) {
    return "active".equalsIgnoreCase(String.valueOf(status).trim());
  }
}
