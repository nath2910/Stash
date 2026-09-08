package backend.controller;

import backend.entity.User;
import backend.dto.UserMapper;
import backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class AccountDataController {
  private final JdbcTemplate jdbc;
  private final ObjectMapper mapper;
  private final UserRepository users;
  public AccountDataController(JdbcTemplate jdbc, ObjectMapper mapper, UserRepository users) {
    this.jdbc = jdbc; this.mapper = mapper; this.users = users;
  }

  public record ProfileRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(max = 100) String firstName,
      @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(max = 100) String lastName) {}

  @PutMapping("/profile")
  @org.springframework.transaction.annotation.Transactional
  public Object profile(@AuthenticationPrincipal User principal, @RequestBody @jakarta.validation.Valid ProfileRequest request) {
    principal = users.lockById(principal.getId()).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED));
    principal.setFirstName(request.firstName().trim());
    principal.setLastName(request.lastName().trim());
    return UserMapper.toMe(users.save(principal));
  }

  @GetMapping("/export")
  public ResponseEntity<?> export(@AuthenticationPrincipal User user) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("account", rows("SELECT (to_jsonb(u) - 'password' - 'session_version')::text FROM users u WHERE id = ?", user.getId()));
    // Table names below are a fixed server-side list, never supplied by a client.
    for (String table : List.of("tableauventes", "admin_states", "admin_invoices", "user_stats_layouts", "notifications", "parcels")) {
      result.put(table, rows("SELECT to_jsonb(t)::text FROM " + table + " t WHERE user_id = ? LIMIT 10001", user.getId()));
    }
    result.put("mail_accounts", rows("SELECT jsonb_build_object('id', id, 'provider', provider, 'email', email_address, 'status', status)::text FROM mail_accounts WHERE user_id = ?", user.getId()));
    result.put("attachments", rows("SELECT (to_jsonb(a) - 'storage_key')::text FROM attachments a WHERE user_id = ? LIMIT 10001", user.getId()));
    return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=stash-donnees.json").body(result);
  }

  private List<JsonNode> rows(String sql, Long userId) {
    var result = jdbc.query(sql, (rs, row) -> {
      try { return mapper.readTree(rs.getString(1)); }
      catch (java.io.IOException ex) { throw new IllegalStateException("Invalid stored JSON"); }
    }, userId);
    if (result.size() > 10000) throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "Export volumineux : demandez un export complet au responsable des données");
    return result;
  }
}
