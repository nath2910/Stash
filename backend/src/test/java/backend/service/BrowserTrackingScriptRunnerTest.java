package backend.service;

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
  }

  private String normalize(Path path) {
    return String.valueOf(path).replace('\\', '/');
  }
}
