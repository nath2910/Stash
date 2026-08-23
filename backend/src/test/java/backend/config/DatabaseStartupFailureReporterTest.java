package backend.config;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DatabaseStartupFailureReporterTest {

  @Test
  void detectsConfiguredLocalPostgresConnectionRefusal() {
    Throwable error =
        new RuntimeException(
            "startup failed",
            new IllegalStateException(
                "Connection to 127.0.0.1:5433 refused. Check that the hostname and port are correct."));

    Assertions.assertTrue(DatabaseStartupFailureReporter.isLocalPostgresConnectionFailure(error));
  }

  @Test
  void ignoresUnrelatedStartupFailures() {
    Throwable error = new RuntimeException("validation failed");

    Assertions.assertFalse(DatabaseStartupFailureReporter.isLocalPostgresConnectionFailure(error));
  }

  @Test
  void detectsConfiguredLocalPostgresConnectionTimeout() {
    Throwable error =
        new RuntimeException(
            "startup failed",
            new IllegalStateException(
                "Unable to obtain connection from database: jdbc:postgresql://127.0.0.1:5433/snkProjet_docker",
                new SocketTimeoutException("Read timed out")));

    Assertions.assertTrue(DatabaseStartupFailureReporter.isLocalPostgresConnectionFailure(error));
  }
}
