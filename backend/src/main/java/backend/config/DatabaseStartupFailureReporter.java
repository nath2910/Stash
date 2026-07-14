package backend.config;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;

public class DatabaseStartupFailureReporter implements ApplicationListener<ApplicationFailedEvent> {
  private static final Logger log = LoggerFactory.getLogger(DatabaseStartupFailureReporter.class);

  @Override
  public void onApplicationEvent(ApplicationFailedEvent event) {
    if (!isLocalPostgresConnectionFailure(event.getException())) {
      return;
    }

    log.error(
        """

        Local PostgreSQL is not reachable on 127.0.0.1:5433.
        The dev profile loads SPRING_DATASOURCE_URL from backend/.env and expects the Docker database from docker-compose.yml.
        Start Docker Desktop, then run: docker compose up -d db
        If you use a different local database port, update backend/.env or SPRING_DATASOURCE_URL before restarting the backend.
        """);
  }

  static boolean isLocalPostgresConnectionFailure(Throwable error) {
    Throwable current = error;
    while (current != null) {
      String message = current.getMessage();
      if (message != null) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        if (normalizedMessage.contains("connection to 127.0.0.1:5433 refused")
            || normalizedMessage.contains("jdbc:postgresql://127.0.0.1:5433")) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }
}
