package backend.dto;

import java.util.List;
import java.util.Map;

public record AdministrativeInvoiceBatchResponse(
    int generatedCount,
    List<Map<String, Object>> invoices,
    AdministrativeDocumentRecordResponse record
) {}
