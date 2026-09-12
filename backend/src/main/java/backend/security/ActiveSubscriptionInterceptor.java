package backend.security;

import backend.entity.User;
import backend.service.BillingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ActiveSubscriptionInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(ActiveSubscriptionInterceptor.class);

  private final SubscriptionAccessService subscriptionAccessService;
  private final BillingService billingService;

  public ActiveSubscriptionInterceptor(SubscriptionAccessService subscriptionAccessService, BillingService billingService) {
    this.subscriptionAccessService = subscriptionAccessService;
    this.billingService = billingService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    if (!requiresActiveSubscription(handlerMethod)) {
      return true;
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    Object principal = authentication == null ? null : authentication.getPrincipal();
    User user = principal instanceof User ? (User) principal : null;
    if (subscriptionAccessService.hasActiveSubscription(user)) {
      return true;
    }
    refreshStripeState(user);
    if (subscriptionAccessService.hasActiveSubscription(user)) {
      return true;
    }
    subscriptionAccessService.requireActiveSubscription(user);
    return true;
  }

  private boolean requiresActiveSubscription(HandlerMethod handlerMethod) {
    return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), RequiresActiveSubscription.class)
        || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), RequiresActiveSubscription.class);
  }

  private void refreshStripeState(User user) {
    if (user == null || user.getId() == null || user.getStripeCustomerId() == null || user.getStripeCustomerId().isBlank()) {
      return;
    }
    try {
      billingService.refreshStatus(user);
    } catch (Exception e) {
      log.warn("Unable to refresh Stripe access before protected request for user {}", user.getId());
    }
  }
}
