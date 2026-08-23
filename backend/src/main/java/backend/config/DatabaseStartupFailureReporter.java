package backend.config;

import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.context.ApplicationListener;

public class DatabaseStartupFailureReporter implements ApplicationListener<ApplicationFailedEvent> {
  private static final Logger log = LoggerFactory.getLogger(DatabaseStartupFailureReporter.class);
  private static final String LOCAL_DEV_DATASOURCE = "127.0.0.1:5433";

  @Override
  public void onApplicationEvent(ApplicationFailedEvent event) {
    if (!isLocalPostgresConnectionFailure(event.getException())) {
      return;
    }

    log.error(
        """

        Local PostgreSQL for the dev profile is not reachable on 127.0.0.1:5433.
        The JPA repository / entityManagerFactory errors above are a consequence: Flyway could not open the datasource first.
        The local dev datasource should usually look like:
          jdbc:postgresql://127.0.0.1:5433/snkProjet_docker?sslmode=disable&connectTimeout=5&socketTimeout=30
        If you use the repo root compose file, start the database with: docker compose up -d db
        If you use backend/src/main/resources/compose.yaml instead, run: docker compose -f backend/src/main/resources/compose.yaml up -d postgres
        If you override SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME or SPRING_DATASOURCE_PASSWORD in backend/.env, verify those values before restarting the backend.
        """);
  }

  static boolean isLocalPostgresConnectionFailure(Throwable error) {
    Throwable current = error;
    boolean mentionsLocalDevDatasource = false;
    boolean mentionsConnectionFailure = false;

    while (current != null) {
      String message = current.getMessage();
      if (message != null) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);

        mentionsLocalDevDatasource =
            mentionsLocalDevDatasource
                || normalizedMessage.contains(LOCAL_DEV_DATASOURCE)
                || normalizedMessage.contains("jdbc:postgresql://" + LOCAL_DEV_DATASOURCE);

        mentionsConnectionFailure =
            mentionsConnectionFailure
                || normalizedMessage.contains("refused")
                || normalizedMessage.contains("timed out")
                || normalizedMessage.contains("la tentative de connexion a échoué")
                || normalizedMessage.contains("la tentative de connexion a echoue")
                || normalizedMessage.contains("unable to obtain connection from database");
      }
      current = current.getCause();
    }

    return mentionsLocalDevDatasource && mentionsConnectionFailure;
  }
}
