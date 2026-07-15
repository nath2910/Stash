package backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BrowserTrackingScriptRunner {

  private static final Logger log = LoggerFactory.getLogger(BrowserTrackingScriptRunner.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final long SCRIPT_TIMEOUT_SECONDS = 25;

  private BrowserTrackingScriptRunner() {
  }

  static Optional<BrowserPagePayload> run(String scriptFileName, String trackingUrl) {
    if (scriptFileName == null || scriptFileName.isBlank() || trackingUrl == null || trackingUrl.isBlank()) {
      return Optional.empty();
    }

    Path scriptPath = resolveScriptPath(scriptFileName);
    if (scriptPath == null) {
      return Optional.empty();
    }

    Process process = null;
    try {
      process = new ProcessBuilder("node", scriptPath.toString(), trackingUrl)
          .redirectErrorStream(true)
          .start();

      boolean finished = process.waitFor(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new IllegalStateException("Browser tracking script timed out: " + scriptFileName);
      }

      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
      if (process.exitValue() != 0) {
        throw new IllegalStateException(output.isBlank() ? "Browser tracking script failed" : output);
      }
      if (output.isBlank()) {
        return Optional.empty();
      }
      return Optional.of(OBJECT_MAPPER.readValue(output, BrowserPagePayload.class));
    } catch (Exception ex) {
      log.warn("Browser tracking script {} failed for {}", scriptFileName, trackingUrl, ex);
      return Optional.empty();
    } finally {
      if (process != null) {
        process.destroy();
      }
    }
  }

  private static Path resolveScriptPath(String scriptFileName) {
    List<Path> candidates = List.of(
        Path.of("").toAbsolutePath().resolve("../frontend/scripts/" + scriptFileName).normalize(),
        Path.of("").toAbsolutePath().resolve("frontend/scripts/" + scriptFileName).normalize()
    );
    for (Path candidate : candidates) {
      if (Files.exists(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  record BrowserPagePayload(
      String source,
      String title,
      String text,
      String html,
      String currentUrl
  ) {
  }
}
