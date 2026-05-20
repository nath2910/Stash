package backend.service;

import backend.dto.TrackingWebhookResponse;
import backend.entity.Parcel;

public interface TrackingAggregatorService {

  void registerTracking(Parcel parcel);

  Parcel refreshTracking(Parcel parcel);

  TrackingWebhookResponse handleWebhook(String payload, String signatureHeader, String sharedSecretHeader);
}
