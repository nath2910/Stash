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
        "Request rejected"
    );
    return ResponseEntity.status(ex.getStatusCode())
        .body(Map.of(
            "error", "request_failed",
            "message", ex.getStatusCode().is5xxServerError() ? "Service temporairement indisponible"
                : ex.getReason() == null ? "Requête refusée" : ex.getReason()
        ));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handle(Exception ex, HttpServletRequest request) {
    log.error(
        "Unhandled API error on {} {}",
        request == null ? "UNKNOWN" : request.getMethod(),
        request == null ? "UNKNOWN" : request.getRequestURI(),
        ex.getClass().getSimpleName()
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of(
            "error", "internal_error",
            "message", "Une erreur interne est survenue. Merci de reessayer plus tard."
        ));
  }

  @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
      org.springframework.http.converter.HttpMessageNotReadableException.class,
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
      org.springframework.web.bind.MissingServletRequestParameterException.class,
      org.springframework.web.bind.MissingRequestHeaderException.class,
      jakarta.validation.ConstraintViolationException.class,
      org.springframework.web.method.annotation.HandlerMethodValidationException.class})
  public ResponseEntity<?> invalidInput(Exception ex) {
    return ResponseEntity.badRequest().body(Map.of("error", "invalid_input", "message", "Données invalides ou incomplètes"));
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  public ResponseEntity<?> oversizedUpload(Exception ex) {
    return ResponseEntity.status(413).body(Map.of("message", "Fichier trop volumineux"));
  }
}
