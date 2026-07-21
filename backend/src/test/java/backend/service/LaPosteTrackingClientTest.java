package backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class LaPosteTrackingClientTest {

  @Test
  void doesNotBlockProdStartupWhenLaposteSourceIsMissing() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");

    LaPosteTrackingClient client = new LaPosteTrackingClient(
        "",
        "https://api.laposte.fr/suivi/v2/idships",
        new ObjectMapper(),
        environment
    );

    Assertions.assertDoesNotThrow(client::validateProductionConfig);
  }
}
