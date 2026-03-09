package backend.dto;

public record DiscordEligibilityResponse(boolean eligible, String status, String reason) {}
