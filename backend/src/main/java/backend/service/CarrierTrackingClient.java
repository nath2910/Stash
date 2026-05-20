package backend.service;

import backend.entity.Parcel;
import java.util.Optional;

public interface CarrierTrackingClient {

  boolean supports(Parcel parcel);

  boolean isConfigured();

  Optional<TrackingSnapshot> fetchTracking(Parcel parcel);
}
