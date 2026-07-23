package backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
        Path configuredExecutable = resolveConfiguredBrowserExecutable(configuredPath);
        if (configuredExecutable != null) {
          return configuredExecutable;
        }
      } catch (Exception ignored) {
        // Fall back to built-in candidates.
      }
    }
    return browserExecutableCandidates().stream()
        .filter(Files::exists)
        .findFirst()
        .orElseGet(BrowserTrackingScriptRunner::resolveBrowserExecutableFromPath);
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
    candidates.add(Path.of("C:/Program Files/Chromium/Application/chrome.exe"));
    candidates.add(Path.of("C:/Program Files/Chromium/chrome.exe"));
    candidates.add(Path.of("/opt/google/chrome/chrome"));
    candidates.add(Path.of("/usr/bin/chromium"));
    candidates.add(Path.of("/usr/bin/chromium-browser"));
    candidates.add(Path.of("/usr/lib/chromium/chromium"));
    candidates.add(Path.of("/usr/lib/chromium-browser/chromium-browser"));
    candidates.add(Path.of("/usr/bin/google-chrome"));
    candidates.add(Path.of("/usr/bin/google-chrome-stable"));
    candidates.add(Path.of("/usr/bin/microsoft-edge"));
    candidates.add(Path.of("/usr/bin/microsoft-edge-stable"));
    candidates.add(Path.of("/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"));
    candidates.add(Path.of("/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"));
    candidates.add(Path.of("/snap/bin/chromium"));
    return candidates;
  }

  static List<String> browserCommandCandidates() {
    return List.of(
        "msedge.exe",
        "msedge",
        "chrome.exe",
        "chrome",
        "chromium.exe",
        "chromium",
        "chromium-browser",
        "google-chrome",
        "google-chrome-stable",
        "microsoft-edge",
        "microsoft-edge-stable"
    );
  }

  static List<Path> pathExecutableCandidates(String pathValue, String pathExtValue) {
    if (pathValue == null || pathValue.isBlank()) {
      return List.of();
    }

    Set<Path> candidates = new LinkedHashSet<>();
    List<String> executableExtensions = executableExtensions(pathExtValue);
    for (String entry : pathValue.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
      String normalizedEntry = stripQuotes(entry);
      if (normalizedEntry == null || normalizedEntry.isBlank()) {
        continue;
      }

      Path directory;
      try {
        directory = Path.of(normalizedEntry);
      } catch (Exception ignored) {
        continue;
      }

      for (String command : browserCommandCandidates()) {
        for (String candidateName : executableNameCandidates(command, executableExtensions)) {
          try {
            candidates.add(directory.resolve(candidateName).normalize());
          } catch (Exception ignored) {
            // Ignore malformed PATH entries and keep scanning.
          }
        }
      }
    }
    return List.copyOf(candidates);
  }

  private static Path resolveConfiguredBrowserExecutable(String configuredPath) {
    String normalizedPath = stripQuotes(configuredPath);
    if (normalizedPath == null || normalizedPath.isBlank()) {
      return null;
    }
    Path configuredExecutable = Path.of(normalizedPath);
    if (Files.exists(configuredExecutable)) {
      return configuredExecutable;
    }
    if (!normalizedPath.contains("/") && !normalizedPath.contains("\\")) {
      return resolveBrowserExecutableFromPath(normalizedPath);
    }
    return null;
  }

  private static Path resolveBrowserExecutableFromPath() {
    return resolveBrowserExecutableFromPath(null);
  }

  private static Path resolveBrowserExecutableFromPath(String preferredCommand) {
    List<Path> candidates = preferredCommand == null || preferredCommand.isBlank()
        ? pathExecutableCandidates(System.getenv("PATH"), System.getenv("PATHEXT"))
        : pathExecutableCandidatesForCommand(preferredCommand, System.getenv("PATH"), System.getenv("PATHEXT"));
    return candidates.stream().filter(Files::exists).findFirst().orElse(null);
  }

  private static List<Path> pathExecutableCandidatesForCommand(
      String command,
      String pathValue,
      String pathExtValue
  ) {
    if (command == null || command.isBlank() || pathValue == null || pathValue.isBlank()) {
      return List.of();
    }

    Set<Path> candidates = new LinkedHashSet<>();
    List<String> executableExtensions = executableExtensions(pathExtValue);
    for (String entry : pathValue.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
      String normalizedEntry = stripQuotes(entry);
      if (normalizedEntry == null || normalizedEntry.isBlank()) {
        continue;
      }

      Path directory;
      try {
        directory = Path.of(normalizedEntry);
      } catch (Exception ignored) {
        continue;
      }

      for (String candidateName : executableNameCandidates(command, executableExtensions)) {
        try {
          candidates.add(directory.resolve(candidateName).normalize());
        } catch (Exception ignored) {
          // Ignore malformed PATH entries and keep scanning.
        }
      }
    }
    return List.copyOf(candidates);
  }

  private static List<String> executableExtensions(String pathExtValue) {
    if (!isWindows()) {
      return List.of();
    }
    String rawValue = pathExtValue == null || pathExtValue.isBlank()
        ? ".EXE;.CMD;.BAT;.COM"
        : pathExtValue;
    List<String> extensions = new ArrayList<>();
    for (String extension : rawValue.split(";")) {
      if (extension == null || extension.isBlank()) {
        continue;
      }
      String normalized = extension.trim().toLowerCase(Locale.ROOT);
      if (!normalized.startsWith(".")) {
        normalized = "." + normalized;
      }
      extensions.add(normalized);
    }
    return extensions;
  }

  private static List<String> executableNameCandidates(String command, List<String> executableExtensions) {
    if (command == null || command.isBlank()) {
      return List.of();
    }

    Set<String> candidates = new LinkedHashSet<>();
    String normalizedCommand = command.trim();
    candidates.add(normalizedCommand);
    if (isWindows() && !hasKnownExecutableExtension(normalizedCommand)) {
      for (String extension : executableExtensions) {
        candidates.add(normalizedCommand + extension);
      }
    }
    return List.copyOf(candidates);
  }

  private static boolean hasKnownExecutableExtension(String command) {
    String normalizedCommand = command.toLowerCase(Locale.ROOT);
    return normalizedCommand.endsWith(".exe")
        || normalizedCommand.endsWith(".cmd")
        || normalizedCommand.endsWith(".bat")
        || normalizedCommand.endsWith(".com");
  }

  private static boolean isWindows() {
    return File.pathSeparatorChar == ';';
  }

  private static String stripQuotes(String value) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() >= 2 && normalized.startsWith("\"") && normalized.endsWith("\"")) {
      return normalized.substring(1, normalized.length() - 1);
    }
    return normalized;
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
