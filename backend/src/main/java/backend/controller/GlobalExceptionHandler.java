package backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex) {
    return ResponseEntity.status(ex.getStatusCode())
        .body(Map.of(
            "error", ex.getClass().getSimpleName(),
            "message", ex.getReason() == null ? "No message" : ex.getReason()
        ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handle(Exception ex) {
    ex.printStackTrace(); // âœ… affiche la vraie cause dans la console
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of(
            "error", ex.getClass().getSimpleName(),
            "message", ex.getMessage() == null ? "No message" : ex.getMessage()
        ));
  }
}
