package backend.service;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class BrowserTrackingScriptRunnerTest {

  @Test
  void scriptPathCandidatesIncludeBackendTrackingScriptsDirectory() {
    List<Path> candidates = BrowserTrackingScriptRunner.scriptPathCandidates("laposte-browser-scrape.mjs");

    Assertions.assertTrue(
        candidates.stream().anyMatch(path -> normalize(path).endsWith("/tracking-scripts/laposte-browser-scrape.mjs")),
        "backend tracking scripts directory should be considered before frontend fallbacks"
    );
  }

  @Test
  void browserExecutableCandidatesIncludeLinuxPathsForProd() {
    List<Path> candidates = BrowserTrackingScriptRunner.browserExecutableCandidates();

    Assertions.assertTrue(
        candidates.stream().map(this::normalize).anyMatch("/usr/bin/chromium"::equals),
        "Linux chromium path should be supported"
    );
    Assertions.assertTrue(
        candidates.stream().map(this::normalize).anyMatch("/usr/bin/google-chrome"::equals),
        "Linux chrome path should be supported"
    );
    Assertions.assertTrue(
        candidates.stream().map(this::normalize).anyMatch("/opt/google/chrome/chrome"::equals),
        "Alternate Linux chrome path should be supported"
    );
  }

  @Test
  void browserCommandCandidatesIncludePathResolvableNames() {
    List<String> candidates = BrowserTrackingScriptRunner.browserCommandCandidates();

    Assertions.assertTrue(candidates.contains("chromium"), "Chromium command should be supported");
    Assertions.assertTrue(candidates.contains("google-chrome"), "Google Chrome command should be supported");
  }

  @Test
  void pathExecutableCandidatesExpandPathDirectories() {
    List<Path> candidates = BrowserTrackingScriptRunner.pathExecutableCandidates(
        String.join(File.pathSeparator, "/opt/bin", "/usr/local/bin"),
        null
    );

    Assertions.assertTrue(
        candidates.stream().map(this::normalize).anyMatch("/opt/bin/chromium"::equals),
        "PATH lookup should include chromium from PATH entries"
    );
    Assertions.assertTrue(
        candidates.stream().map(this::normalize).anyMatch("/usr/local/bin/google-chrome"::equals),
        "PATH lookup should include google-chrome from PATH entries"
    );
  }

  @Test
  void unavailableReasonMentionsMissingTrackingScripts() {
    String reason = BrowserTrackingScriptRunner.unavailableReason("missing-script.mjs");

    Assertions.assertEquals("scripts de tracking absents du runtime backend", reason);
  }

  private String normalize(Path path) {
    return String.valueOf(path).replace('\\', '/');
  }
}
