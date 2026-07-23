package backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class BrowserTrackingScriptRunner {

  private static final Logger log = LoggerFactory.getLogger(BrowserTrackingScriptRunner.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final long SCRIPT_TIMEOUT_SECONDS = 60;

  private BrowserTrackingScriptRunner() {
  }

  static boolean isAvailable(String scriptFileName) {
    return availability(scriptFileName).available();
  }

  static String unavailableReason(String scriptFileName) {
    Availability availability = availability(scriptFileName);
    return availability.available() ? null : availability.reason();
  }

  static Optional<BrowserPagePayload> run(String scriptFileName, String trackingUrl) {
    if (scriptFileName == null || scriptFileName.isBlank() || trackingUrl == null || trackingUrl.isBlank()) {
      return Optional.empty();
    }

    Path scriptPath = resolveScriptPath(scriptFileName);
    if (scriptPath == null) {
      return Optional.empty();
    }

    try {
      Process process = new ProcessBuilder("node", scriptPath.toString(), trackingUrl)
          .directory(scriptPath.getParent().toFile())
          .redirectErrorStream(true)
          .start();

      StringBuilder outputBuffer = new StringBuilder();
      Thread outputReader = new Thread(() -> readProcessOutput(process, outputBuffer));
      outputReader.setDaemon(true);
      outputReader.start();

      boolean finished = process.waitFor(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new IllegalStateException("Browser tracking script timed out: " + scriptFileName);
      }

      outputReader.join(1000);
      String output = outputBuffer.toString().trim();
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
    }
  }

  private static Path resolveScriptPath(String scriptFileName) {
    for (Path candidate : scriptPathCandidates(scriptFileName)) {
      if (Files.exists(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  static Availability availability(String scriptFileName) {
    if (scriptFileName == null || scriptFileName.isBlank()) {
      return new Availability(false, "nom de script de tracking vide", null, null);
    }

    Path scriptPath = resolveScriptPath(scriptFileName);
    if (scriptPath == null) {
      return new Availability(false, "scripts de tracking absents du runtime backend", null, null);
    }

    Path browserExecutable = resolveBrowserExecutable();
    if (browserExecutable == null) {
      return new Availability(false, "Chrome ou Chromium absent du runtime backend", scriptPath, null);
    }

    return new Availability(true, null, scriptPath, browserExecutable);
  }

  private static Path resolveBrowserExecutable() {
    String configuredPath = System.getenv("PUPPETEER_EXECUTABLE_PATH");
    if (configuredPath != null && !configuredPath.isBlank()) {
      try {
        if (Files.exists(Path.of(configuredPath))) {
          return Path.of(configuredPath);
        }
      } catch (Exception ignored) {
        // Fall back to built-in candidates.
      }
    }
    return browserExecutableCandidates().stream().filter(Files::exists).findFirst().orElse(null);
  }

  static List<Path> scriptPathCandidates(String scriptFileName) {
    Path cwd = Path.of("").toAbsolutePath();
    return List.of(
        cwd.resolve("tracking-scripts/" + scriptFileName).normalize(),
        cwd.resolve("../tracking-scripts/" + scriptFileName).normalize(),
        cwd.resolve("backend/tracking-scripts/" + scriptFileName).normalize(),
        cwd.resolve("../frontend/scripts/" + scriptFileName).normalize(),
        cwd.resolve("frontend/scripts/" + scriptFileName).normalize()
    );
  }

  static List<Path> browserExecutableCandidates() {
    List<Path> candidates = new ArrayList<>();
    candidates.add(Path.of("C:/Program Files/Microsoft/Edge/Application/msedge.exe"));
    candidates.add(Path.of("C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe"));
    candidates.add(Path.of("C:/Program Files/Google/Chrome/Application/chrome.exe"));
    candidates.add(Path.of("C:/Program Files (x86)/Google/Chrome/Application/chrome.exe"));
    candidates.add(Path.of("/usr/bin/chromium"));
    candidates.add(Path.of("/usr/bin/chromium-browser"));
    candidates.add(Path.of("/usr/bin/google-chrome"));
    candidates.add(Path.of("/usr/bin/google-chrome-stable"));
    candidates.add(Path.of("/snap/bin/chromium"));
    return candidates;
  }

  private static void readProcessOutput(Process process, StringBuilder outputBuffer) {
    if (process == null || outputBuffer == null) {
      return;
    }
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
    )) {
      String line;
      boolean firstLine = true;
      while ((line = reader.readLine()) != null) {
        if (!firstLine) {
          outputBuffer.append(System.lineSeparator());
        }
        outputBuffer.append(line);
        firstLine = false;
      }
    } catch (Exception ignored) {
      // Best-effort stream draining to avoid child process blocking on a full pipe.
    }
  }

  record BrowserPagePayload(
      String source,
      String title,
      String text,
      String html,
      String currentUrl
  ) {
  }

  record Availability(
      boolean available,
      String reason,
      Path scriptPath,
      Path browserExecutable
  ) {
  }
}
