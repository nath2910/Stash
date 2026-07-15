package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

@Service
@Order(120)
public class UpsTrackingClient implements CarrierTrackingClient {

  private static final String PROVIDER = "UPS_BROWSER_PAGE";
  private static final String SCRIPT_NAME = "ups-browser-scrape.mjs";
  private static final Pattern UPS_PATTERN = Pattern.compile("^1Z[0-9A-Z]{16}$", Pattern.CASE_INSENSITIVE);

  @Override
  public boolean supports(Parcel parcel) {
    if (parcel == null) {
      return false;
    }
    String carrier = normalizedCarrier(parcel);
    if ("ups".equals(carrier)) {
      return true;
    }
    String trackingUrl = rawTrackingUrl(parcel);
    if ("ups".equals(TrackingLinkResolver.detectCarrierSlug(trackingUrl))) {
      return true;
    }
    String tracking = normalizedTracking(parcel);
    return UPS_PATTERN.matcher(tracking).matches();
  }

  @Override
  public boolean isConfigured() {
    return true;
  }

  @Override
  public Optional<TrackingSnapshot> fetchTracking(Parcel parcel) {
    return BrowserTrackingScriptRunner.run(SCRIPT_NAME, trackingPageUrl(parcel))
        .filter(payload -> !PublicTrackingPageClient.looksLikeBotChallenge(payload.html()))
        .map(payload -> toSnapshot(parcel, payload))
        .filter(snapshot -> snapshot.status() != ParcelStatus.UNKNOWN || snapshot.statusLabel() != null);
  }

  static TrackingSnapshot toSnapshot(Parcel parcel, BrowserTrackingScriptRunner.BrowserPagePayload payload) {
    List<TrackingEventSnapshot> events = TrackingBrowserPageSupport.extractUpsEvents(payload.text());
    String statusLabel = TrackingBrowserPageSupport.bestStatusLabel(payload.text(), payload.title());
    ParcelStatus status = TrackingBrowserPageSupport.resolveBestStatus(payload.html(), payload.text(), events);
    OffsetDateTime deliveredAt = TrackingBrowserPageSupport.latestDeliveredAt(events);
    if (deliveredAt == null && status == ParcelStatus.DELIVERED) {
      deliveredAt = TrackingBrowserPageSupport.latestEventTime(events);
    }

    Map<String, Object> rawPayload = new HashMap<>();
    rawPayload.put("mode", "browser_tracking_page");
    rawPayload.put("tracking_url", TrackingBrowserPageSupport.firstNonBlank(payload.currentUrl(), trackingPageUrl(parcel)));
    putIfPresent(rawPayload, "page_title", payload.title());
    putIfPresent(rawPayload, "page_text_excerpt", excerpt(payload.text(), 4000));
    putIfPresent(rawPayload, "page_html_excerpt", excerpt(payload.html(), 8000));
    putIfPresent(rawPayload, "source", payload.source());

    return new TrackingSnapshot(
        PROVIDER,
        TrackingBrowserPageSupport.firstNonBlank(parcel.getNormalizedTrackingNumber(), parcel.getTrackingNumber()),
        "ups",
        status,
        statusLabel,
        null,
        deliveredAt,
        TrackingBrowserPageSupport.firstNonBlank(payload.currentUrl(), trackingPageUrl(parcel)),
        null,
        null,
        "UPS",
        null,
        rawPayload,
        events
    );
  }

  private static String normalizedCarrier(Parcel parcel) {
    if (parcel == null || parcel.getCarrierSlug() == null) {
      return "";
    }
    String normalized = parcel.getCarrierSlug().trim().toLowerCase(Locale.ROOT);
    return "unknown".equals(normalized) ? "" : normalized;
  }

  private static String normalizedTracking(Parcel parcel) {
    String value = TrackingBrowserPageSupport.firstNonBlank(
        parcel == null ? null : parcel.getNormalizedTrackingNumber(),
        parcel == null ? null : parcel.getTrackingNumber()
    );
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  private static String rawTrackingUrl(Parcel parcel) {
    if (parcel == null || parcel.getRawCurrentPayload() == null) {
      return null;
    }
    Object rawUrl = parcel.getRawCurrentPayload().get("tracking_url");
    return rawUrl == null ? null : String.valueOf(rawUrl);
  }

  private static String trackingPageUrl(Parcel parcel) {
    String trackingNumber = TrackingBrowserPageSupport.firstNonBlank(
        parcel == null ? null : parcel.getTrackingNumber(),
        parcel == null ? null : parcel.getNormalizedTrackingNumber()
    );
    if (trackingNumber == null) {
      return null;
    }
    return "https://www.ups.com/track?tracknum="
        + URLEncoder.encode(trackingNumber.trim(), StandardCharsets.UTF_8);
  }

  private static String excerpt(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String compact = TrackingBrowserPageSupport.compact(value);
    return compact.length() <= maxLength ? compact : compact.substring(0, maxLength);
  }

  private static void putIfPresent(Map<String, Object> target, String key, String value) {
    if (value != null && !value.isBlank()) {
      target.put(key, value);
    }
  }
}
