package backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de healthcheck simple (GET/HEAD) accessible sans authentification.
 * Utile pour les plateformes type Koyeb/Vercel qui utilisent HEAD/GET.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

  @GetMapping
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("ok");
  }
}
