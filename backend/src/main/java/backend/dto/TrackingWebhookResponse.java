package backend.dto;

public record TrackingWebhookResponse(boolean accepted, boolean processed, String message) {
}
