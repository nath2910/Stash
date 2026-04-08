package backend.dto;

import java.util.List;

public record NotificationPageResponse(
    List<NotificationResponse> items,
    int page,
    int size,
    long total,
    boolean hasNext,
    long unreadCount
) {}
