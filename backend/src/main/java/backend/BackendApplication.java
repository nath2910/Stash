
package backend;
import backend.config.DatabaseStartupFailureReporter;
import backend.config.StripeProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableConfigurationProperties(StripeProperties.class)
public class BackendApplication {
  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(BackendApplication.class);
    application.addListeners(new DatabaseStartupFailureReporter());
    application.run(args);
  }
}

  
