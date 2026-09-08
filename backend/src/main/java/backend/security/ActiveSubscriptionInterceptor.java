package backend.security;

import backend.entity.User;
import backend.service.DiscordAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ActiveSubscriptionInterceptor implements HandlerInterceptor {

  private final SubscriptionAccessService subscriptionAccessService;
  private final DiscordAccessService discordAccessService;

  public ActiveSubscriptionInterceptor(SubscriptionAccessService subscriptionAccessService, DiscordAccessService discordAccessService) {
    this.subscriptionAccessService = subscriptionAccessService;
    this.discordAccessService = discordAccessService;
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
    subscriptionAccessService.requireActiveSubscription(user);
    return true;
  }

  private boolean requiresActiveSubscription(HandlerMethod handlerMethod) {
    return AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), RequiresActiveSubscription.class)
        || AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), RequiresActiveSubscription.class);
  }
}
