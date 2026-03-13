package backend.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import backend.dto.SnkVenteCreateDto;
import backend.dto.SnkVenteImportDto;
import backend.dto.TopVenteProjection;
import backend.dto.AttachmentDto;
import backend.entity.SnkVente;
import backend.entity.User;
import backend.repository.SnkVenteRepository.BrandCount;
import backend.service.AttachmentService;
import backend.service.snkVenteService;

@RestController
@RequestMapping(path = "snkVente")
public class snkVenteController {

  private final snkVenteService snkVenteService;
  private final AttachmentService attachmentService;

  public snkVenteController(snkVenteService snkVenteService, AttachmentService attachmentService) {
    this.snkVenteService = snkVenteService;
    this.attachmentService = attachmentService;
  }

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public SnkVente creer(
      @AuthenticationPrincipal User currentUser,
      @RequestBody @jakarta.validation.Valid SnkVenteCreateDto dto
  ) {
    return snkVenteService.creer(userId(currentUser), dto);
  }

  @GetMapping(path = "{id}", produces = APPLICATION_JSON_VALUE)
  public SnkVente lire(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Integer id
  ) {
    return snkVenteService.lire(userId(currentUser), id);
  }

  @GetMapping(produces = APPLICATION_JSON_VALUE)
  public List<SnkVente> rechercher(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) Integer limit
  ) {
    return snkVenteService.rechercherParUser(userId(currentUser), limit);
  }

  @GetMapping("/recent")
  public List<SnkVente> getDernieresVentes(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(defaultValue = "7") int limit
  ) {
    return snkVenteService.getDernieresVentesParUser(userId(currentUser), limit);
  }

  @PostMapping("/add")
  @ResponseStatus(HttpStatus.CREATED)
  public void ajouterPaire(
      @AuthenticationPrincipal User currentUser,
      @RequestBody @jakarta.validation.Valid SnkVenteCreateDto dto
  ) {
    snkVenteService.creer(userId(currentUser), dto);
  }

  @GetMapping("/total")
  public BigDecimal total(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) Integer year
  ) {
    Long userId = userId(currentUser);
    if (year != null) return snkVenteService.totalBenefYear(userId, year);
    return snkVenteService.totalBenef(userId);
  }

  @GetMapping("/ca")
  public BigDecimal sumPrixResell(@AuthenticationPrincipal User currentUser) {
    return snkVenteService.sumPrixResell(userId(currentUser));
  }

  @GetMapping("/marque")
  public List<BrandCount> marque(@AuthenticationPrincipal User currentUser) {
    return snkVenteService.graphMarque(userId(currentUser));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteVente(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Integer id
  ) {
    snkVenteService.deleteVente(userId(currentUser), id);
  }

  @PostMapping(path = "/bulk-delete", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Integer>> deleteBulk(
      @AuthenticationPrincipal User currentUser,
      @RequestBody List<Integer> ids
  ) {
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("deleted", 0));
    }
    int deleted = snkVenteService.deleteBulk(userId(currentUser), ids);
    return ResponseEntity.ok(Map.of("deleted", deleted));
  }

  @GetMapping("/topVentes")
  public List<TopVenteProjection> topVentes(@AuthenticationPrincipal User currentUser) {
    return snkVenteService.getTop3VentesAnneeCourante(userId(currentUser));
  }

  @PutMapping(path = "{id}", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public SnkVente update(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Integer id,
      @RequestBody @jakarta.validation.Valid SnkVente payload
  ) {
    return snkVenteService.updateVente(userId(currentUser), id, payload);
  }

  @PostMapping(path = "/import", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
  public ResponseEntity<Map<String, Integer>> importCsv(
      @AuthenticationPrincipal User currentUser,
      @RequestBody @jakarta.validation.Valid List<SnkVenteImportDto> items
  ) {
    if (currentUser == null) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("created", 0));
    }

    int created = snkVenteService.importBulk(currentUser.getId(), items);
    return ResponseEntity.ok(Map.of("created", created));
  }

  @GetMapping("/{id}/attachments")
  public List<AttachmentDto> listAttachments(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Integer id
  ) {
    return attachmentService.list(userId(currentUser), id).stream().map(AttachmentDto::fromEntity).toList();
  }

  @PostMapping(path = "/{id}/attachments", consumes = {"multipart/form-data"})
  public AttachmentDto uploadAttachment(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Integer id,
      @RequestPart("file") MultipartFile file
  ) {
    return AttachmentDto.fromEntity(attachmentService.add(userId(currentUser), id, file));
  }

  @DeleteMapping("/{id}/attachments/{attachmentId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteAttachment(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Integer id,
      @PathVariable Long attachmentId
  ) {
    attachmentService.delete(userId(currentUser), id, attachmentId);
  }

  @GetMapping("/{id}/attachments/{attachmentId}/download")
  public ResponseEntity<Resource> downloadAttachment(
      @AuthenticationPrincipal User currentUser,
      @PathVariable Integer id,
      @PathVariable Long attachmentId
  ) {
    Resource resource = attachmentService.download(userId(currentUser), id, attachmentId);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(resource);
  }

  private Long userId(User currentUser) {
    return currentUser.getId();
  }
}
