package backend.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import backend.entity.User;
import backend.service.AdminService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "admin")
public class AdminController {

  private final AdminService adminService;

  public AdminController(AdminService adminService) {
    this.adminService = adminService;
  }

  @GetMapping(path = "dashboard", produces = APPLICATION_JSON_VALUE)
  public Map<String, Object> dashboard(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to
  ) {
    return adminService.dashboard(userId(currentUser), from, to);
  }

  @GetMapping(path = "state", produces = APPLICATION_JSON_VALUE)
  public Map<String, Object> state(@AuthenticationPrincipal User currentUser) {
    return adminService.state(userId(currentUser));
  }

  @PutMapping(path = "state", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public Map<String, Object> saveState(
      @AuthenticationPrincipal User currentUser,
      @RequestBody Map<String, Object> payload
  ) {
    return adminService.saveState(currentUser, payload);
  }

  @GetMapping(path = "settings", produces = APPLICATION_JSON_VALUE)
  public Map<String, Object> settings(@AuthenticationPrincipal User currentUser) {
    return adminService.settings(userId(currentUser));
  }

  @PutMapping(path = "settings", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public Map<String, Object> saveSettings(
      @AuthenticationPrincipal User currentUser,
      @RequestBody Map<String, Object> payload
  ) {
    return adminService.saveSettings(currentUser, payload);
  }

  @GetMapping(path = "invoices", produces = APPLICATION_JSON_VALUE)
  public List<Map<String, Object>> invoices(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(defaultValue = "100") int limit
  ) {
    return adminService.invoices(userId(currentUser), limit);
  }

  @PostMapping(path = "invoices", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> createInvoice(
      @AuthenticationPrincipal User currentUser,
      @RequestBody Map<String, Object> payload
  ) {
    return adminService.createInvoice(currentUser, payload);
  }

  @PatchMapping(path = "invoices/{id}/paid", produces = APPLICATION_JSON_VALUE)
  public Map<String, Object> markInvoicePaid(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Long id
  ) {
    return adminService.markInvoicePaid(userId(currentUser), id);
  }

  @PatchMapping(path = "invoices/{id}/cancel", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public Map<String, Object> cancelInvoice(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Long id,
      @RequestBody(required = false) Map<String, Object> payload
  ) {
    String reason = payload == null ? "" : String.valueOf(payload.getOrDefault("reason", ""));
    return adminService.cancelInvoice(userId(currentUser), id, reason);
  }

  @GetMapping(path = "export.csv", produces = "text/csv")
  public ResponseEntity<String> exportCsv(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to
  ) {
    String csv = adminService.exportCsv(userId(currentUser), from, to);
    String filename = "export-administratif-" + LocalDate.now() + ".csv";
    return ResponseEntity.ok()
        .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(csv);
  }

  private Long userId(User currentUser) {
    return currentUser.getId();
  }
}
