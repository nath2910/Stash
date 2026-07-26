package backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
    log.warn(
        "API request failed with status {} on {} {}: {}",
        ex.getStatusCode().value(),
        request == null ? "UNKNOWN" : request.getMethod(),
        request == null ? "UNKNOWN" : request.getRequestURI(),
        ex.getReason()
    );
    return ResponseEntity.status(ex.getStatusCode())
        .body(Map.of(
            "error", ex.getClass().getSimpleName(),
            "message", ex.getReason() == null ? "No message" : ex.getReason()
        ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handle(Exception ex, HttpServletRequest request) {
    log.error(
        "Unhandled API error on {} {}",
        request == null ? "UNKNOWN" : request.getMethod(),
        request == null ? "UNKNOWN" : request.getRequestURI(),
        ex
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of(
            "error", ex.getClass().getSimpleName(),
            "message", "Une erreur interne est survenue. Merci de reessayer plus tard."
        ));
  }
}
