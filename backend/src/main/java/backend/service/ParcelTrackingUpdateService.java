package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelEvent;
import backend.entity.ParcelStatus;
import backend.repository.ParcelEventRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ParcelTrackingUpdateService {

  private final ParcelEventRepository parcelEventRepository;

  public ParcelTrackingUpdateService(ParcelEventRepository parcelEventRepository) {
    this.parcelEventRepository = parcelEventRepository;
  }

  public void applySnapshot(Parcel parcel, TrackingSnapshot snapshot) {
    if (parcel == null || snapshot == null) {
      return;
    }

    if (snapshot.provider() != null && !snapshot.provider().isBlank()) {
      parcel.setAggregator(snapshot.provider());
    }
    if (snapshot.providerTrackingId() != null && !snapshot.providerTrackingId().isBlank()) {
      parcel.setAggregatorTrackingId(snapshot.providerTrackingId());
    }
    if (snapshot.carrierSlug() != null && !snapshot.carrierSlug().isBlank()) {
      parcel.setCarrierSlug(snapshot.carrierSlug());
    }
    boolean appliedStatus = false;
    if (snapshot.status() != null) {
      ParcelStatus currentStatus = parcel.getStatus();
      ParcelStatus incomingStatus = resolveIncomingStatus(snapshot);
      if (shouldApplyStatus(currentStatus, incomingStatus)) {
        parcel.setStatus(incomingStatus);
        appliedStatus = true;
      }
    } else if (parcel.getStatus() == null || parcel.getStatus() == ParcelStatus.PENDING) {
      parcel.setStatus(ParcelStatus.REGISTERED);
      appliedStatus = true;
    }
    String resolvedLabel = resolvedStatusLabel(snapshot, parcel.getStatus());
    if ((appliedStatus || parcel.getStatusLabel() == null || parcel.getStatusLabel().isBlank())
        && resolvedLabel != null
        && !resolvedLabel.isBlank()) {
      parcel.setStatusLabel(resolvedLabel);
    }
    if (snapshot.estimatedDeliveryAt() != null) {
      parcel.setEstimatedDeliveryAt(snapshot.estimatedDeliveryAt());
    }
    if (snapshot.deliveredAt() != null) {
      parcel.setDeliveredAt(snapshot.deliveredAt());
    }

    Map<String, Object> rawPayload = snapshot.rawPayload() == null
        ? new HashMap<>()
        : new HashMap<>(snapshot.rawPayload());
    putIfPresent(rawPayload, "tracking_url", snapshot.trackingUrl());
    putIfPresent(rawPayload, "origin_raw_location", snapshot.originAddress());
    putIfPresent(rawPayload, "destination_raw_location", snapshot.destinationAddress());
    putIfPresent(rawPayload, "shipment_type", snapshot.shipmentType());
    putIfPresent(rawPayload, "signed_by", snapshot.signedBy());
    parcel.setRawCurrentPayload(rawPayload);

    OffsetDateTime latest = null;
    if (snapshot.events() != null) {
      for (TrackingEventSnapshot eventSnapshot : snapshot.events()) {
        OffsetDateTime eventTime = eventSnapshot.eventTime() == null
            ? OffsetDateTime.now(ZoneOffset.UTC)
            : eventSnapshot.eventTime();
        latest = latest == null || eventTime.toInstant().isAfter(latest.toInstant()) ? eventTime : latest;
        insertEvent(parcel, eventSnapshot, eventTime);
      }
    }

    if (latest != null) {
      parcel.setLastEventAt(latest);
    }
    if (parcel.getStatus() == ParcelStatus.DELIVERED && parcel.getDeliveredAt() == null) {
      parcel.setDeliveredAt(parcel.getLastEventAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : parcel.getLastEventAt());
    }
  }

  public void markLocalFallback(Parcel parcel, String provider, String label) {
    if (parcel == null) {
      return;
    }
    if (provider != null && !provider.isBlank()) {
      parcel.setAggregator(provider);
    }
    if (parcel.getStatus() == null || parcel.getStatus() == ParcelStatus.PENDING) {
      parcel.setStatus(ParcelStatus.REGISTERED);
    }
    if (label != null && !label.isBlank()) {
      parcel.setStatusLabel(label);
    } else if (parcel.getStatusLabel() == null || parcel.getStatusLabel().isBlank()) {
      parcel.setStatusLabel("Suivi enregistre");
    }
  }

  private void insertEvent(Parcel parcel, TrackingEventSnapshot snapshot, OffsetDateTime eventTime) {
    ParcelStatus status = snapshot.status() == null ? ParcelStatus.UNKNOWN : snapshot.status();
    String eventHash = hashEvent(
        parcel.getId(),
        status,
        snapshot.substatus(),
        snapshot.description(),
        snapshot.location(),
        eventTime
    );
    if (parcelEventRepository.existsByParcel_IdAndEventHash(parcel.getId(), eventHash)) {
      return;
    }

    ParcelEvent event = new ParcelEvent();
    event.setParcel(parcel);
    event.setEventHash(eventHash);
    event.setStatus(status);
    event.setSubstatus(snapshot.substatus());
    event.setDescription(snapshot.description());
    event.setLocation(snapshot.location());
    event.setEventTime(eventTime);
    event.setRawPayload(snapshot.rawPayload() == null ? Map.of() : new HashMap<>(snapshot.rawPayload()));

    try {
      parcelEventRepository.save(event);
    } catch (DataIntegrityViolationException ignored) {
      // Idempotence in concurrent refreshes/webhooks.
    }
  }

  private String hashEvent(
      Long parcelId,
      ParcelStatus status,
      String substatus,
      String description,
      String location,
      OffsetDateTime eventTime
  ) {
    String input = parcelId + "|" + status + "|" + safe(substatus) + "|" + safe(description)
        + "|" + safe(location) + "|" + eventTime.toInstant();
    try {
      return toHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to hash parcel event", ex);
    }
  }

  private void putIfPresent(Map<String, Object> target, String key, String value) {
    if (value != null && !value.isBlank()) {
      target.put(key, value);
    }
  }

  private String toHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  private String safe(String value) {
    return value == null ? "" : value;
  }

  private ParcelStatus resolveIncomingStatus(TrackingSnapshot snapshot) {
    if (snapshot == null) {
      return null;
    }
    if (snapshot.deliveredAt() != null) {
      return ParcelStatus.DELIVERED;
    }
    if (CarrierStatusResolver.resolve(snapshot.statusLabel()) == ParcelStatus.DELIVERED) {
      return ParcelStatus.DELIVERED;
    }
    if (snapshot.events() != null) {
      for (TrackingEventSnapshot event : snapshot.events()) {
        if (event == null) {
          continue;
        }
        if (event.status() == ParcelStatus.DELIVERED) {
          return ParcelStatus.DELIVERED;
        }
        if (CarrierStatusResolver.resolve(event.description(), event.substatus(), event.location()) == ParcelStatus.DELIVERED) {
          return ParcelStatus.DELIVERED;
        }
      }
    }
    return snapshot.status();
  }

  private String resolvedStatusLabel(TrackingSnapshot snapshot, ParcelStatus appliedStatus) {
    if (snapshot == null) {
      return null;
    }

    List<String> candidates = new ArrayList<>();
    candidates.add(snapshot.statusLabel());
    if (snapshot.events() != null) {
      for (TrackingEventSnapshot event : snapshot.events()) {
        if (event == null) {
          continue;
        }
        candidates.add(event.description());
        candidates.add(event.substatus());
      }
    }

    if (appliedStatus == ParcelStatus.DELIVERED) {
      for (String candidate : candidates) {
        if (CarrierStatusResolver.resolve(candidate) == ParcelStatus.DELIVERED) {
          return candidate;
        }
      }
    }

    for (String candidate : candidates) {
      if (candidate != null && !candidate.isBlank()) {
        return candidate;
      }
    }
    return null;
  }

  private boolean shouldApplyStatus(ParcelStatus currentStatus, ParcelStatus incomingStatus) {
    if (incomingStatus == null) {
      return false;
    }
    if (currentStatus == null || currentStatus == ParcelStatus.PENDING || currentStatus == ParcelStatus.UNKNOWN) {
      return true;
    }
    if (incomingStatus == currentStatus) {
      return true;
    }
    if (currentStatus == ParcelStatus.DELIVERED && incomingStatus != ParcelStatus.DELIVERED) {
      return false;
    }
    if (incomingStatus == ParcelStatus.EXCEPTION) {
      return currentStatus != ParcelStatus.DELIVERED;
    }
    return statusPriority(incomingStatus) >= statusPriority(currentStatus);
  }

  private int statusPriority(ParcelStatus status) {
    if (status == null) {
      return 0;
    }
    return switch (status) {
      case PENDING, UNKNOWN -> 0;
      case REGISTERED -> 1;
      case IN_TRANSIT -> 2;
      case OUT_FOR_DELIVERY -> 3;
      case EXCEPTION -> 4;
      case DELIVERED -> 5;
    };
  }
}
