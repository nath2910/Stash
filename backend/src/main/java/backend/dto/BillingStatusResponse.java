package backend.dto;

public record BillingStatusResponse(String status, String portalUrl,
    java.time.OffsetDateTime currentPeriodEnd, boolean cancelAtPeriodEnd, boolean hasAccess) {}
