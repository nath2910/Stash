package backend.dto;

import backend.entity.ParcelEvent;
import backend.entity.ParcelStatus;
import java.time.OffsetDateTime;

public record ParcelEventResponse(
    Long id,
    ParcelStatus status,
    String substatus,
    String description,
    String location,
    OffsetDateTime eventTime,
    OffsetDateTime createdAt
) {
  public static ParcelEventResponse fromEntity(ParcelEvent event) {
    return new ParcelEventResponse(
        event.getId(),
        event.getStatus(),
        event.getSubstatus(),
        event.getDescription(),
        event.getLocation(),
        event.getEventTime(),
        event.getCreatedAt()
    );
  }
}
