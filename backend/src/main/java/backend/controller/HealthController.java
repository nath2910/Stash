package backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de healthcheck/keep-alive simples (GET/HEAD) accessibles sans authentification.
 * Ils ne font aucun appel base de donnees.
 */
@RestController
public class HealthController {

  @GetMapping({"/health", "/ping"})
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("ok");
  }
}
