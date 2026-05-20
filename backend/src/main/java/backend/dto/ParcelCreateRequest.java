package backend.dto;

public record ParcelCreateRequest(
    String trackingNumber,
    String carrierSlug
) {
}
