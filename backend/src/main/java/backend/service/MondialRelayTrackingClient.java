package backend.service;

import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Service
@Order(110)
public class MondialRelayTrackingClient implements CarrierTrackingClient {

  private static final String PROVIDER = "MONDIAL_RELAY_DIRECT";
  private static final String BROWSER_PROVIDER = "MONDIAL_RELAY_BROWSER_PAGE";
  private static final String BROWSER_SCRIPT = "mondialrelay-browser-scrape.mjs";
  private static final String SOAP_ACTION = "http://www.mondialrelay.fr/webservice/WSI2_TracingColisDetaille";
  private static final Pattern MONDIAL_RELAY_LIKE = Pattern.compile("\\d{8,12}");
  private static final DateTimeFormatter MR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final String enseigne;
  private final String privateKey;
  private final String endpoint;
  private final RestClient restClient;

  public MondialRelayTrackingClient(
      @Value("${app.delivery.mondial-relay-enseigne:}") String enseigne,
      @Value("${app.delivery.mondial-relay-private-key:}") String privateKey,
      @Value("${app.delivery.mondial-relay-endpoint:https://www.mondialrelay.fr/WebService/WebService.asmx}") String endpoint
  ) {
    this.enseigne = enseigne == null ? "" : enseigne.trim();
    this.privateKey = privateKey == null ? "" : privateKey.trim();
    this.endpoint = endpoint == null || endpoint.isBlank()
        ? "https://www.mondialrelay.fr/WebService/WebService.asmx"
        : endpoint.trim();
    this.restClient = RestClient.builder().build();
  }

  @Override
  public boolean supports(Parcel parcel) {
    String carrier = parcel.getCarrierSlug() == null ? "" : parcel.getCarrierSlug().trim().toLowerCase(Locale.ROOT);
    if (carrier.equals("mondial-relay") || carrier.equals("mondialrelay")) {
      return true;
    }
    String tracking = parcel.getNormalizedTrackingNumber() == null ? "" : parcel.getNormalizedTrackingNumber();
    return MONDIAL_RELAY_LIKE.matcher(tracking).matches();
  }

  @Override
  public boolean isConfigured() {
    return !enseigne.isBlank() && !privateKey.isBlank();
  }

  @Override
  public Optional<TrackingSnapshot> fetchTracking(Parcel parcel) {
    if (isConfigured()) {
      try {
        String response = restClient.post()
            .uri(endpoint)
            .contentType(MediaType.parseMediaType("text/xml; charset=utf-8"))
            .header("SOAPAction", SOAP_ACTION)
            .body(soapEnvelope(parcel.getTrackingNumber()))
            .retrieve()
            .body(String.class);

        if (response != null && !response.isBlank()) {
          TrackingSnapshot snapshot = toSnapshot(parcel, parseXml(response));
          if (snapshot != null) {
            return Optional.of(snapshot);
          }
        }
      } catch (Exception ignored) {
        // Browser fallback below.
      }
    }

    return fetchFromBrowserPage(parcel);
  }

  private TrackingSnapshot toSnapshot(Parcel parcel, Document document) {
    Element result = firstElement(document, "WSI2_TracingColisDetailleResult");
    if (result == null) {
      return null;
    }

    List<TrackingEventSnapshot> events = new ArrayList<>();
    NodeList tracingNodes = result.getElementsByTagName("ret_WSI2_sub_TracingColisDetaille");
    for (int i = 0; i < tracingNodes.getLength(); i++) {
      if (!(tracingNodes.item(i) instanceof Element eventElement)) {
        continue;
      }
      String label = text(eventElement, "Libelle");
      String location = firstNonBlank(
          text(eventElement, "Emplacement"),
          compactLocation(text(eventElement, "Relais_Num"), text(eventElement, "Relais_Pays"))
      );
      OffsetDateTime eventTime = parseMondialRelayDateTime(text(eventElement, "Date"), text(eventElement, "Heure"));
      Map<String, Object> rawEvent = new HashMap<>();
      putIfPresent(rawEvent, "Libelle", label);
      putIfPresent(rawEvent, "Date", text(eventElement, "Date"));
      putIfPresent(rawEvent, "Heure", text(eventElement, "Heure"));
      putIfPresent(rawEvent, "Emplacement", text(eventElement, "Emplacement"));
      putIfPresent(rawEvent, "Relais_Num", text(eventElement, "Relais_Num"));
      putIfPresent(rawEvent, "Relais_Pays", text(eventElement, "Relais_Pays"));

      events.add(new TrackingEventSnapshot(
          normalizeStatus(label),
          null,
          label,
          location,
          eventTime,
          rawEvent
      ));
    }

    TrackingEventSnapshot latest = latestEvent(events);
    String destination = compactLocation(text(result, "Relais_Libelle"), text(result, "Relais_Num"));
    ResolvedStatus resolvedStatus = resolveStatus(
        events,
        text(result, "Libelle02"),
        text(result, "Libelle01")
    );

    Map<String, Object> rawPayload = new HashMap<>();
    putIfPresent(rawPayload, "Libelle01", text(result, "Libelle01"));
    putIfPresent(rawPayload, "Libelle02", text(result, "Libelle02"));
    putIfPresent(rawPayload, "Relais_Libelle", text(result, "Relais_Libelle"));
    putIfPresent(rawPayload, "Relais_Num", text(result, "Relais_Num"));

    return new TrackingSnapshot(
        PROVIDER,
        parcel.getNormalizedTrackingNumber(),
        "mondial-relay",
        resolvedStatus.status(),
        resolvedStatus.label(),
        null,
        resolvedStatus.status() == ParcelStatus.DELIVERED && latest != null ? latest.eventTime() : null,
        fallbackTrackingUrl(parcel),
        null,
        destination,
        "Mondial Relay",
        null,
        rawPayload,
        events
    );
  }

  private String soapEnvelope(String trackingNumber) {
    String expedition = trackingNumber == null ? "" : trackingNumber.trim();
    String langue = "FR";
    String security = md5Upper(enseigne + expedition + langue + privateKey);
    return """
        <?xml version="1.0" encoding="utf-8"?>
        <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
          <soap:Body>
            <WSI2_TracingColisDetaille xmlns="http://www.mondialrelay.fr/webservice/">
              <Enseigne>%s</Enseigne>
              <Expedition>%s</Expedition>
              <Langue>%s</Langue>
              <Security>%s</Security>
            </WSI2_TracingColisDetaille>
          </soap:Body>
        </soap:Envelope>
        """.formatted(xmlEscape(enseigne), xmlEscape(expedition), langue, security);
  }

  private Document parseXml(String xml) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setXIncludeAware(false);
      factory.setExpandEntityReferences(false);
      return factory.newDocumentBuilder()
          .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to parse Mondial Relay response", ex);
    }
  }

  private ParcelStatus normalizeStatus(String value) {
    return normalizeStatusStatic(value);
  }

  static TrackingEventSnapshot latestEvent(List<TrackingEventSnapshot> events) {
    if (events == null || events.isEmpty()) {
      return null;
    }
    return events.stream()
        .filter(event -> event != null && event.eventTime() != null)
        .max(Comparator.comparing(TrackingEventSnapshot::eventTime))
        .orElse(events.get(events.size() - 1));
  }

  static ResolvedStatus resolveStatus(
      List<TrackingEventSnapshot> events,
      String primaryLabel,
      String secondaryLabel
  ) {
    List<ResolvedStatus> candidates = new ArrayList<>();
    if (events != null) {
      for (TrackingEventSnapshot event : events) {
        if (event != null) {
          candidates.add(new ResolvedStatus(event.status(), event.description()));
        }
      }
    }
    if (primaryLabel != null && !primaryLabel.isBlank()) {
      candidates.add(new ResolvedStatus(normalizeStatusStatic(primaryLabel), primaryLabel));
    }
    if (secondaryLabel != null && !secondaryLabel.isBlank()) {
      candidates.add(new ResolvedStatus(normalizeStatusStatic(secondaryLabel), secondaryLabel));
    }

    ResolvedStatus resolved = candidates.stream()
        .filter(candidate -> candidate.status() != null && candidate.status() != ParcelStatus.UNKNOWN)
        .max(Comparator.comparingInt(candidate -> statusPriority(candidate.status())))
        .orElseGet(() -> {
          TrackingEventSnapshot latest = latestEvent(events == null ? List.of() : events);
          String fallbackLabel = firstNonBlankStatic(
              latest == null ? null : latest.description(),
              primaryLabel,
              secondaryLabel
          );
          return new ResolvedStatus(normalizeStatusStatic(fallbackLabel), fallbackLabel);
        });
    return preferSummaryLabel(resolved, primaryLabel, secondaryLabel);
  }

  private OffsetDateTime parseMondialRelayDateTime(String dateValue, String timeValue) {
    if (dateValue == null || dateValue.isBlank()) {
      return null;
    }
    try {
      LocalDate date = LocalDate.parse(dateValue.trim(), MR_DATE);
      LocalTime time = parseTime(timeValue);
      return date.atTime(time).atOffset(ZoneOffset.UTC);
    } catch (Exception ignored) {
      return null;
    }
  }

  private LocalTime parseTime(String value) {
    if (value == null || value.isBlank()) {
      return LocalTime.MIDNIGHT;
    }
    String normalized = value.trim().replace('h', ':').replace('H', ':');
    if (normalized.matches("\\d{1,2}:\\d{2}")) {
      return LocalTime.parse(normalized.length() == 4 ? "0" + normalized : normalized);
    }
    if (normalized.matches("\\d{4}")) {
      return LocalTime.of(Integer.parseInt(normalized.substring(0, 2)), Integer.parseInt(normalized.substring(2, 4)));
    }
    return LocalTime.MIDNIGHT;
  }

  private Element firstElement(Document document, String name) {
    NodeList nodes = document.getElementsByTagName(name);
    if (nodes.getLength() == 0 || !(nodes.item(0) instanceof Element element)) {
      return null;
    }
    return element;
  }

  private String text(Element element, String name) {
    NodeList nodes = element.getElementsByTagName(name);
    if (nodes.getLength() == 0) {
      return null;
    }
    Node node = nodes.item(0);
    if (node == null) {
      return null;
    }
    String value = node.getTextContent();
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String fallbackTrackingUrl(Parcel parcel) {
    return "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition="
        + URLEncoder.encode(parcel.getTrackingNumber().trim(), StandardCharsets.UTF_8);
  }

  private static String browserFallbackTrackingUrl(Parcel parcel) {
    String trackingNumber = parcel == null ? null : TrackingBrowserPageSupport.firstNonBlank(
        parcel.getTrackingNumber(),
        parcel.getNormalizedTrackingNumber()
    );
    if (trackingNumber == null) {
      return null;
    }
    return "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition="
        + URLEncoder.encode(trackingNumber.trim(), StandardCharsets.UTF_8);
  }

  private Optional<TrackingSnapshot> fetchFromBrowserPage(Parcel parcel) {
    return BrowserTrackingScriptRunner.run(BROWSER_SCRIPT, browserFallbackTrackingUrl(parcel))
        .filter(payload -> !PublicTrackingPageClient.looksLikeBotChallenge(payload.html()))
        .map(payload -> toBrowserSnapshot(parcel, payload))
        .filter(snapshot -> snapshot.status() != ParcelStatus.UNKNOWN || snapshot.statusLabel() != null);
  }

  static TrackingSnapshot toBrowserSnapshot(Parcel parcel, BrowserTrackingScriptRunner.BrowserPagePayload payload) {
    List<TrackingEventSnapshot> events = TrackingBrowserPageSupport.extractEuropeanEvents(payload.text());
    String statusLabel = TrackingBrowserPageSupport.bestStatusLabel(payload.text(), payload.title());
    ParcelStatus status = TrackingBrowserPageSupport.resolveBestStatus(payload.html(), payload.text(), events);
    OffsetDateTime deliveredAt = TrackingBrowserPageSupport.latestDeliveredAt(events);
    if (deliveredAt == null && status == ParcelStatus.DELIVERED) {
      deliveredAt = TrackingBrowserPageSupport.latestEventTime(events);
    }

    Map<String, Object> rawPayload = new HashMap<>();
    rawPayload.put("mode", "browser_tracking_page");
    rawPayload.put("tracking_url", TrackingBrowserPageSupport.firstNonBlank(payload.currentUrl(), browserFallbackTrackingUrl(parcel)));
    putIfPresent(rawPayload, "page_title", payload.title());
    putIfPresent(rawPayload, "page_text_excerpt", excerpt(payload.text(), 4000));
    putIfPresent(rawPayload, "page_html_excerpt", excerpt(payload.html(), 8000));
    putIfPresent(rawPayload, "source", payload.source());

    return new TrackingSnapshot(
        BROWSER_PROVIDER,
        parcel.getNormalizedTrackingNumber(),
        "mondial-relay",
        status,
        statusLabel,
        null,
        deliveredAt,
        TrackingBrowserPageSupport.firstNonBlank(payload.currentUrl(), browserFallbackTrackingUrl(parcel)),
        null,
        null,
        "Mondial Relay",
        null,
        rawPayload,
        events
    );
  }

  private String md5Upper(String value) {
    try {
      byte[] digest = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(digest.length * 2);
      for (byte b : digest) {
        sb.append(String.format("%02X", b));
      }
      return sb.toString();
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to sign Mondial Relay request", ex);
    }
  }

  private String compactLocation(String... parts) {
    StringBuilder sb = new StringBuilder();
    if (parts == null) {
      return null;
    }
    for (String part : parts) {
      if (part == null || part.isBlank()) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(part.trim());
    }
    return sb.length() == 0 ? null : sb.toString();
  }

  private static void putIfPresent(Map<String, Object> target, String key, String value) {
    if (value != null && !value.isBlank()) {
      target.put(key, value);
    }
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private String xmlEscape(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  private static ParcelStatus normalizeStatusStatic(String value) {
    return CarrierStatusResolver.resolve(value);
  }

  private static String firstNonBlankStatic(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return null;
  }

  private static int statusPriority(ParcelStatus status) {
    if (status == null) {
      return 0;
    }
    return switch (status) {
      case UNKNOWN, PENDING -> 0;
      case REGISTERED -> 1;
      case IN_TRANSIT -> 2;
      case OUT_FOR_DELIVERY -> 3;
      case EXCEPTION -> 4;
      case DELIVERED -> 5;
    };
  }

  private static ResolvedStatus preferSummaryLabel(ResolvedStatus resolved, String primaryLabel, String secondaryLabel) {
    if (resolved == null || resolved.status() == null) {
      return resolved;
    }
    if (primaryLabel != null && normalizeStatusStatic(primaryLabel) == resolved.status()) {
      return new ResolvedStatus(resolved.status(), primaryLabel);
    }
    if (secondaryLabel != null && normalizeStatusStatic(secondaryLabel) == resolved.status()) {
      return new ResolvedStatus(resolved.status(), secondaryLabel);
    }
    return resolved;
  }

  record ResolvedStatus(ParcelStatus status, String label) {
  }

  private static String excerpt(String value, int maxLength) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String compact = TrackingBrowserPageSupport.compact(value);
    return compact.length() <= maxLength ? compact : compact.substring(0, maxLength);
  }

}
