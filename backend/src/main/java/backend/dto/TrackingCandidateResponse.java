package backend.dto;

import backend.entity.MailTrackingCandidate;
import backend.entity.TrackingCandidateStatus;
import backend.service.TrackingLinkResolver;
import java.time.OffsetDateTime;

public record TrackingCandidateResponse(
    Long id,
    String trackingNumber,
    String normalizedTrackingNumber,
    String carrierSlug,
    String trackingUrl,
    String merchantName,
    String rawStatus,
    String sourceSender,
    String sourceSubject,
    OffsetDateTime receivedAt,
    String contextSnippet,
    int confidenceScore,
    String confidenceLevel,
    TrackingCandidateStatus status,
    String reason,
    Long parcelId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
  public static TrackingCandidateResponse fromEntity(MailTrackingCandidate candidate) {
    return new TrackingCandidateResponse(
        candidate.getId(),
        candidate.getTrackingNumber(),
        candidate.getNormalizedTrackingNumber(),
        candidate.getCarrierSlug(),
        TrackingLinkResolver.preferredTrackingUrl(
            candidate.getTrackingUrl(),
            candidate.getCarrierSlug(),
            candidate.getNormalizedTrackingNumber()
        ),
        candidate.getMerchantName(),
        candidate.getRawStatus(),
        candidate.getSourceSender(),
        candidate.getSourceSubject(),
        candidate.getReceivedAt(),
        candidate.getContextSnippet(),
        candidate.getConfidenceScore(),
        candidate.getConfidenceLevel(),
        candidate.getStatus(),
        candidate.getReason(),
        candidate.getParcel() == null ? null : candidate.getParcel().getId(),
        candidate.getCreatedAt(),
        candidate.getUpdatedAt()
    );
  }
}
