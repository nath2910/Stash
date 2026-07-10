package backend.dto;

import java.util.List;

public record MailScanBatchResponse(
    int scannedAccounts,
    int scannedMessages,
    int deliveryMessages,
    int importedCount,
    int reviewCount,
    int rejectedCount,
    int duplicateCount,
    String message,
    List<MailScanResponse> accountReports,
    List<TrackingCandidateResponse> candidatesToReview,
    List<MailScanParcelResponse> importedParcels,
    List<MailScanParcelResponse> duplicateParcels
) {
}
