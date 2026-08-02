package backend.config;

import backend.security.ActiveSubscriptionInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final ActiveSubscriptionInterceptor activeSubscriptionInterceptor;

  public WebMvcConfig(ActiveSubscriptionInterceptor activeSubscriptionInterceptor) {
    this.activeSubscriptionInterceptor = activeSubscriptionInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(activeSubscriptionInterceptor);
  }
}
