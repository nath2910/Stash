package backend.service;

import backend.dto.MailScanParcelResponse;
import backend.dto.TrackingCandidateResponse;
import backend.entity.MailAccount;
import backend.entity.MailAccountStatus;
import backend.entity.SeenMailMessage;
import backend.repository.MailAccountRepository;
import backend.repository.SeenMailMessageRepository;
import backend.service.TrackingParserService.TrackingCandidate;
import backend.service.TrackingParserService.TrackingDetectionResult;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class GmailMailScannerService {

  private static final Logger log = LoggerFactory.getLogger(GmailMailScannerService.class);
  private static final String GMAIL_MESSAGES_PATH = "/gmail/v1/users/me/messages";
  private static final int GMAIL_PAGE_SIZE = 100;
  private static final int GMAIL_PAGE_SCAN_LIMIT = 10;
  private static final String BASE_QUERY =
      "(suivi OR tracking OR colis OR livraison OR expedition OR expedie "
          + "OR \"numero de suivi\" OR \"tracking number\" OR shipment OR shipped OR delivery "
          + "OR package OR parcel OR colissimo OR laposte OR \"la poste\" OR chronopost "
          + "OR \"mondial relay\" OR dhl OR ups OR fedex OR dpd OR gls)";
  private static final Pattern PLAIN_TEXT_LINK = Pattern.compile("https?://[^\\s<>()\"']+", Pattern.CASE_INSENSITIVE);

  private final int maxResults;
  private final int lookbackDays;
  private final MailAccountRepository mailAccountRepository;
  private final SeenMailMessageRepository seenMailMessageRepository;
  private final TokenEncryptionService tokenEncryptionService;
  private final TrackingParserService trackingParserService;
  private final DeliveryTrackingService deliveryTrackingService;
  private final RestClient restClient;
  private final String googleClientId;
  private final String googleClientSecret;

  public GmailMailScannerService(
      @Value("${app.delivery.gmail-max-results:${app.delivery.scan-batch-size:50}}") int maxResults,
      @Value("${app.delivery.gmail-lookback-days:14}") int lookbackDays,
      @Value("${spring.security.oauth2.client.registration.google.client-id:}") String googleClientId,
      @Value("${spring.security.oauth2.client.registration.google.client-secret:}") String googleClientSecret,
      MailAccountRepository mailAccountRepository,
      SeenMailMessageRepository seenMailMessageRepository,
      TokenEncryptionService tokenEncryptionService,
      TrackingParserService trackingParserService,
      DeliveryTrackingService deliveryTrackingService
  ) {
    this.maxResults = maxResults;
    this.lookbackDays = normalizeLookbackDays(lookbackDays);
    this.googleClientId = googleClientId;
    this.googleClientSecret = googleClientSecret;
    this.mailAccountRepository = mailAccountRepository;
    this.seenMailMessageRepository = seenMailMessageRepository;
    this.tokenEncryptionService = tokenEncryptionService;
    this.trackingParserService = trackingParserService;
    this.deliveryTrackingService = deliveryTrackingService;
    this.restClient = RestClient.builder()
        .baseUrl("https://gmail.googleapis.com")
        .build();
  }

  public int scanAccount(Long mailAccountId) {
    return scanAccountDetailed(mailAccountId).importedCount();
  }

  public MailScanSummary scanAccountDetailed(Long mailAccountId) {
    MailAccount account = mailAccountRepository.findByIdWithUser(mailAccountId)
        .orElseThrow(() -> new IllegalArgumentException("Mail account not found: " + mailAccountId));

    if (account.getStatus() == MailAccountStatus.DISABLED || account.getStatus() == MailAccountStatus.REVOKED) {
      return MailScanSummary.empty();
    }

    try {
      MailScanSummary summary = runScan(account);
      markSuccess(account);
      return summary;
    } catch (Exception ex) {
      markFailure(account);
      log.warn("Gmail scan failed for mailAccount={}", mailAccountId);
      if (ex instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      throw new IllegalStateException("Gmail scan failed", ex);
    }
  }

  private MailScanSummary runScan(MailAccount account) {
    String accessToken = ensureAccessToken(account);
    List<String> messageIds = listRecentMessageIds(account.getId(), accessToken);
    MailScanAccumulator scan = new MailScanAccumulator();
    OffsetDateTime lookbackStart = lookbackStart();

    for (String messageId : messageIds) {
      boolean alreadySeen = seenMailMessageRepository.existsByMailAccount_IdAndProviderMessageId(account.getId(), messageId);

      Map<String, Object> metadata = fetchMessage(accessToken, messageId, "metadata");
      String from = headerValue(metadata, "From");
      String subject = headerValue(metadata, "Subject");
      OffsetDateTime receivedAt = receivedAt(metadata);
      if (receivedAt.isBefore(lookbackStart)) {
        if (!alreadySeen) {
          markSeen(account, messageId, receivedAt);
        }
        continue;
      }

      Map<String, Object> full = fetchMessage(accessToken, messageId, "full");
      EmailScanContent content = extractContentForParsing(full);
      if (!alreadySeen) {
        scan.scannedMessages++;
      }

      if (!trackingParserService.shouldInspectMessage(from, subject, content.visibleText())) {
        if (!alreadySeen) {
          markSeen(account, messageId, receivedAt);
        }
        continue;
      }

      if (!alreadySeen) {
        scan.deliveryMessages++;
      }
      TrackingDetectionResult detection = trackingParserService.detect(
          from,
          subject,
          content.visibleText(),
          content.links()
      );
      if (!alreadySeen) {
        scan.rejectedCount += detection.rejectedCount();
        scan.duplicateCount += detection.duplicateCount();
      }

      for (TrackingCandidate candidate : detection.autoImportCandidates()) {
        DeliveryTrackingService.MailCandidateImportResult result = deliveryTrackingService.importDetectedFromMail(
            account.getUser().getId(),
            account,
            candidate,
            messageId,
            subject,
            from,
            receivedAt
        );
        if (!alreadySeen) {
          if (result.imported()) {
            scan.importedCount++;
            addParcel(scan.importedParcels, result.parcel());
          } else {
            scan.duplicateCount++;
            addParcel(scan.duplicateParcels, result.parcel());
          }
        }
      }

      if (alreadySeen) {
        continue;
      }

      for (TrackingCandidate candidate : detection.reviewCandidates()) {
        TrackingCandidateResponse stored = deliveryTrackingService.storeCandidateForReview(
            account.getUser().getId(),
            account,
            candidate,
            messageId,
            subject,
            from,
            receivedAt
        );
        scan.reviewCount++;
        scan.candidatesToReview.add(stored);
      }
      markSeen(account, messageId, receivedAt);
    }

    return scan.toSummary();
  }

  private String ensureAccessToken(MailAccount account) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    if (account.getEncryptedAccessToken() != null
        && account.getAccessTokenExpiresAt() != null
        && account.getAccessTokenExpiresAt().isAfter(now.plusSeconds(60))) {
      return tokenEncryptionService.decrypt(account.getEncryptedAccessToken());
    }

    String refreshToken = tokenEncryptionService.decrypt(account.getEncryptedRefreshToken());
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", googleClientId);
    form.add("client_secret", googleClientSecret);
    form.add("refresh_token", refreshToken);
    form.add("grant_type", "refresh_token");

    Map<String, Object> response = RestClient.create()
        .post()
        .uri("https://oauth2.googleapis.com/token")
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});

    String accessToken = stringValue(response.get("access_token"));
    if (accessToken == null) {
      throw new IllegalStateException("Gmail refresh did not return an access token");
    }

    account.setEncryptedAccessToken(tokenEncryptionService.encrypt(accessToken));
    account.setAccessTokenExpiresAt(expiresAt(response.get("expires_in")));
    mailAccountRepository.save(account);
    return accessToken;
  }

  @SuppressWarnings("unchecked")
  private List<String> listRecentMessageIds(Long mailAccountId, String accessToken) {
    List<String> ids = new ArrayList<>();
    int targetCount = Math.max(1, maxResults);
    String pageToken = null;
    int scannedPages = 0;

    while (ids.size() < targetCount && scannedPages < GMAIL_PAGE_SCAN_LIMIT) {
      final String currentPageToken = pageToken;
      Map<String, Object> response = restClient.get()
          .uri(uriBuilder -> {
            var builder = uriBuilder
                .path(GMAIL_MESSAGES_PATH)
                .queryParam("q", gmailQuery())
                .queryParam("maxResults", GMAIL_PAGE_SIZE);
            if (currentPageToken != null && !currentPageToken.isBlank()) {
              builder.queryParam("pageToken", currentPageToken);
            }
            return builder.build();
          })
          .headers(headers -> headers.setBearerAuth(accessToken))
          .retrieve()
          .body(new ParameterizedTypeReference<>() {});

      Object messages = response == null ? null : response.get("messages");
      if (!(messages instanceof List<?> list) || list.isEmpty()) {
        break;
      }

      for (Object item : list) {
        if (!(item instanceof Map<?, ?> map)) {
          continue;
        }
        Object id = map.get("id");
        if (id == null) {
          continue;
        }
        String providerMessageId = String.valueOf(id);
        if (!ids.contains(providerMessageId)) {
          ids.add(providerMessageId);
        }
        if (ids.size() >= targetCount) {
          break;
        }
      }

      pageToken = stringValue(response == null ? null : response.get("nextPageToken"));
      scannedPages++;
      if (pageToken == null) {
        break;
      }
    }
    return ids;
  }

  private Map<String, Object> fetchMessage(String accessToken, String messageId, String format) {
    return restClient.get()
        .uri(uriBuilder -> {
          var builder = uriBuilder
              .path(GMAIL_MESSAGES_PATH + "/" + messageId)
              .queryParam("format", format);
          if ("metadata".equals(format)) {
            builder.queryParam("metadataHeaders", "From", "Subject", "Date");
          }
          return builder.build();
        })
        .headers(headers -> headers.setBearerAuth(accessToken))
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
  }

  @SuppressWarnings("unchecked")
  private String headerValue(Map<String, Object> message, String name) {
    Object payload = message.get("payload");
    if (!(payload instanceof Map<?, ?> payloadMap)) {
      return "";
    }
    Object headers = payloadMap.get("headers");
    if (!(headers instanceof List<?> headerList)) {
      return "";
    }
    for (Object header : headerList) {
      if (header instanceof Map<?, ?> headerMap
          && name.equalsIgnoreCase(String.valueOf(headerMap.get("name")))) {
        Object value = headerMap.get("value");
        return value == null ? "" : String.valueOf(value);
      }
    }
    return "";
  }

  private OffsetDateTime receivedAt(Map<String, Object> message) {
    Object internalDate = message.get("internalDate");
    if (internalDate != null) {
      try {
        return OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(Long.parseLong(String.valueOf(internalDate))),
            ZoneOffset.UTC
        );
      } catch (NumberFormatException ignored) {
        return OffsetDateTime.now(ZoneOffset.UTC);
      }
    }
    return OffsetDateTime.now(ZoneOffset.UTC);
  }

  private EmailScanContent extractContentForParsing(Map<String, Object> message) {
    StringBuilder visibleText = new StringBuilder(8192);
    List<String> links = new ArrayList<>();
    Object snippet = message.get("snippet");
    if (snippet != null) {
      visibleText.append(snippet).append('\n');
    }
    Object payload = message.get("payload");
    if (payload instanceof Map<?, ?> payloadMap) {
      appendPayloadText(payloadMap, visibleText, links);
    }
    return new EmailScanContent(
        visibleText.toString().replaceAll("\\s+", " ").trim(),
        links.stream().distinct().toList()
    );
  }

  @SuppressWarnings("unchecked")
  private void appendPayloadText(Map<?, ?> payload, StringBuilder visibleText, List<String> links) {
    Object mimeTypeValue = payload.get("mimeType");
    String mimeType = mimeTypeValue == null ? "" : String.valueOf(mimeTypeValue);
    Object body = payload.get("body");
    String normalizedMimeType = mimeType.toLowerCase(Locale.ROOT);
    if ((normalizedMimeType.startsWith("text/plain") || normalizedMimeType.startsWith("text/html"))
        && body instanceof Map<?, ?> bodyMap) {
      Object data = bodyMap.get("data");
      if (data != null) {
        String decoded = decodeGmailBody(String.valueOf(data));
        if (normalizedMimeType.startsWith("text/html")) {
          appendHtml(decoded, visibleText, links);
        } else {
          visibleText.append(decoded).append('\n');
          links.addAll(extractPlainTextLinks(decoded));
        }
      }
    }

    Object parts = payload.get("parts");
    if (parts instanceof List<?> list) {
      for (Object part : list) {
        if (part instanceof Map<?, ?> partMap) {
          appendPayloadText(partMap, visibleText, links);
        }
      }
    }
  }

  private void appendHtml(String html, StringBuilder visibleText, List<String> links) {
    Document document = Jsoup.parse(html == null ? "" : html);
    document.select("script,style,noscript,template").remove();
    for (Element link : document.select("a[href]")) {
      String href = link.attr("href");
      if (href != null && !href.isBlank()) {
        links.add(href.trim());
      }
    }
    visibleText.append(document.text()).append('\n');
  }

  private List<String> extractPlainTextLinks(String text) {
    List<String> links = new ArrayList<>();
    Matcher matcher = PLAIN_TEXT_LINK.matcher(text == null ? "" : text);
    while (matcher.find()) {
      links.add(matcher.group());
    }
    return links;
  }

  private String decodeGmailBody(String data) {
    try {
      String padded = padBase64(data);
      return new String(Base64.getUrlDecoder().decode(padded), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException ex) {
      return "";
    }
  }

  private void markSeen(MailAccount account, String messageId, OffsetDateTime receivedAt) {
    SeenMailMessage seen = new SeenMailMessage();
    seen.setMailAccount(account);
    seen.setProviderMessageId(messageId);
    seen.setReceivedAt(receivedAt);
    seen.setParsedAt(OffsetDateTime.now(ZoneOffset.UTC));
    try {
      seenMailMessageRepository.saveAndFlush(seen);
    } catch (DataIntegrityViolationException ignored) {
      // Idempotence in concurrent scans.
    }
  }

  private void markSuccess(MailAccount account) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    account.setStatus(MailAccountStatus.ACTIVE);
    account.setErrorCount(0);
    account.setLastScanAt(now);
    account.setNextScanAt(now.plusMinutes(5).plusSeconds(ThreadLocalRandom.current().nextInt(0, 90)));
    mailAccountRepository.save(account);
  }

  private void markFailure(MailAccount account) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    int nextErrorCount = account.getErrorCount() + 1;
    account.setErrorCount(nextErrorCount);
    account.setStatus(nextErrorCount >= 5 ? MailAccountStatus.ERROR : MailAccountStatus.ACTIVE);
    long backoffSeconds = Math.min(3600, 60L * (1L << Math.min(nextErrorCount, 5)));
    account.setNextScanAt(now.plusSeconds(backoffSeconds + ThreadLocalRandom.current().nextInt(0, 90)));
    mailAccountRepository.save(account);
  }

  private OffsetDateTime expiresAt(Object expiresInValue) {
    long expiresIn = 3600L;
    if (expiresInValue instanceof Number number) {
      expiresIn = number.longValue();
    } else if (expiresInValue != null) {
      try {
        expiresIn = Long.parseLong(String.valueOf(expiresInValue));
      } catch (NumberFormatException ignored) {
        expiresIn = 3600L;
      }
    }
    return OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(Math.max(60, expiresIn - 60));
  }

  private String stringValue(Object value) {
    if (value == null) {
      return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() ? null : text;
  }

  private String padBase64(String value) {
    int remainder = value.length() % 4;
    return remainder == 0 ? value : value + "=".repeat(4 - remainder);
  }

  private String gmailQuery() {
    return "newer_than:" + lookbackDays + "d " + BASE_QUERY;
  }

  private OffsetDateTime lookbackStart() {
    return OffsetDateTime.now(ZoneOffset.UTC).minusDays(lookbackDays);
  }

  private int normalizeLookbackDays(int value) {
    return Math.max(1, Math.min(30, value));
  }

  private void addParcel(List<MailScanParcelResponse> target, backend.entity.Parcel parcel) {
    MailScanParcelResponse response = MailScanParcelResponse.fromEntity(parcel);
    if (response == null || response.id() == null) {
      return;
    }
    boolean alreadyPresent = target.stream().anyMatch(item -> response.id().equals(item.id()));
    if (!alreadyPresent) {
      target.add(response);
    }
  }

  public record MailScanSummary(
      int scannedMessages,
      int deliveryMessages,
      int importedCount,
      int reviewCount,
      int rejectedCount,
      int duplicateCount,
      List<TrackingCandidateResponse> candidatesToReview,
      List<MailScanParcelResponse> importedParcels,
      List<MailScanParcelResponse> duplicateParcels
  ) {
    static MailScanSummary empty() {
      return new MailScanSummary(0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of());
    }
  }

  private static class MailScanAccumulator {
    private int scannedMessages;
    private int deliveryMessages;
    private int importedCount;
    private int reviewCount;
    private int rejectedCount;
    private int duplicateCount;
    private final List<TrackingCandidateResponse> candidatesToReview = new ArrayList<>();
    private final List<MailScanParcelResponse> importedParcels = new ArrayList<>();
    private final List<MailScanParcelResponse> duplicateParcels = new ArrayList<>();

    private MailScanSummary toSummary() {
      return new MailScanSummary(
          scannedMessages,
          deliveryMessages,
          importedCount,
          reviewCount,
          rejectedCount,
          duplicateCount,
          List.copyOf(candidatesToReview),
          List.copyOf(importedParcels),
          List.copyOf(duplicateParcels)
      );
    }
  }

  private record EmailScanContent(String visibleText, List<String> links) {
  }
}
