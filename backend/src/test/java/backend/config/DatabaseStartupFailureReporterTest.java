package backend.config;

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
}
