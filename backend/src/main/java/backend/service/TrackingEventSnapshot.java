package backend.service;

import backend.entity.ParcelStatus;
import java.time.OffsetDateTime;
import java.util.Map;

public record TrackingEventSnapshot(
    ParcelStatus status,
    String substatus,
    String description,
    String location,
    OffsetDateTime eventTime,
    Map<String, Object> rawPayload
) {
}
