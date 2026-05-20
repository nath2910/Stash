package backend.dto;

import java.util.List;

public record MailScanResponse(
    MailAccountResponse account,
    int scannedMessages,
    int deliveryMessages,
    int importedCount,
    int reviewCount,
    int rejectedCount,
    int duplicateCount,
    String message,
    List<TrackingCandidateResponse> candidatesToReview
) {
}
