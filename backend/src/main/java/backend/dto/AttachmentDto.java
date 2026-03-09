package backend.dto;

import backend.entity.Attachment;
import java.time.Instant;

public record AttachmentDto(
    Long id,
    String filename,
    String mimeType,
    long sizeBytes,
    Instant createdAt
) {
  public static AttachmentDto fromEntity(Attachment a) {
    return new AttachmentDto(a.getId(), a.getFilename(), a.getMimeType(), a.getSizeBytes(), a.getCreatedAt());
  }
}
