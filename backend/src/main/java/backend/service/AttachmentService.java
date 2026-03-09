package backend.service;

import backend.entity.Attachment;
import backend.entity.ItemType;
import backend.entity.SnkVente;
import backend.entity.User;
import backend.repository.AttachmentRepository;
import backend.repository.SnkVenteRepository;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AttachmentService {

  private static final long MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 MB
  private final AttachmentRepository attachmentRepository;
  private final SnkVenteRepository snkVenteRepository;
  private final FileStorageService storageService;

  public AttachmentService(
      AttachmentRepository attachmentRepository,
      SnkVenteRepository snkVenteRepository,
      FileStorageService storageService) {
    this.attachmentRepository = attachmentRepository;
    this.snkVenteRepository = snkVenteRepository;
    this.storageService = storageService;
  }

  public List<Attachment> list(Long userId, Integer venteId) {
    ensureItemOwnership(userId, venteId);
    return attachmentRepository.findByVente_IdAndUser_Id(venteId, userId);
  }

  public Attachment add(Long userId, Integer venteId, MultipartFile file) {
    SnkVente vente = ensureItemOwnership(userId, venteId);
    validateFileAgainstType(vente.getType(), file);
    FileStorageService.StoredFile stored = storageService.store(file, userId, venteId);

    Attachment att = Attachment.builder()
        .vente(vente)
        .user(vente.getUser())
        .filename(stored.filename())
        .mimeType(stored.mimeType())
        .sizeBytes(stored.sizeBytes())
        .storageKey(stored.storageKey())
        .build();
    return attachmentRepository.save(att);
  }

  public void delete(Long userId, Integer venteId, Long attachmentId) {
    Attachment att = attachmentRepository.findByIdAndVente_IdAndUser_Id(attachmentId, venteId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable ou non autorisée"));
    attachmentRepository.delete(att);
    storageService.delete(att.getStorageKey());
  }

  public Resource download(Long userId, Integer venteId, Long attachmentId) {
    Attachment att = attachmentRepository.findByIdAndVente_IdAndUser_Id(attachmentId, venteId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable ou non autorisée"));
    return storageService.loadAsResource(att.getStorageKey());
  }

  private SnkVente ensureItemOwnership(Long userId, Integer venteId) {
    return snkVenteRepository.findById(venteId)
        .filter(v -> v.getUser() != null && userId.equals(v.getUser().getId()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item introuvable ou non autorisé"));
  }

  private void validateFileAgainstType(ItemType type, MultipartFile file) {
    if (file == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier manquant");
    if (file.getSize() > MAX_FILE_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fichier trop volumineux (max 10MB)");
    }
    String contentType = file.getContentType() != null ? file.getContentType() : "";
    boolean isPdf = contentType.equalsIgnoreCase(MediaType.APPLICATION_PDF_VALUE)
        || file.getOriginalFilename() != null && file.getOriginalFilename().toLowerCase().endsWith(".pdf");
    boolean isImage = contentType.startsWith("image/");

    if (type == ItemType.TICKET) {
      if (!isPdf && !isImage) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pour les tickets, seuls les PDF ou images sont autorisés");
      }
    } else {
      if (!isImage && !isPdf) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seuls les PDF ou images sont autorisés");
      }
    }
  }
}
