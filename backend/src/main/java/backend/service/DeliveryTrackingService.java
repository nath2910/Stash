package backend.service;

import backend.dto.ParcelCompletionRequest;
import backend.dto.ParcelResponse;
import backend.dto.ParcelCreateRequest;
import backend.dto.TrackingCandidateResponse;
import backend.entity.MailAccount;
import backend.entity.MailTrackingCandidate;
import backend.entity.Parcel;
import backend.entity.ParcelEvent;
import backend.entity.ParcelStatus;
import backend.entity.TrackingCandidateStatus;
import backend.entity.User;
import backend.repository.MailTrackingCandidateRepository;
import backend.repository.ParcelEventRepository;
import backend.repository.ParcelRepository;
import backend.repository.UserRepository;
import backend.service.TrackingParserService.TrackingCandidate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeliveryTrackingService {

  private static final String UNKNOWN_CARRIER = "unknown";
  private static final String SUPPORTED_TRACKING_MESSAGE =
      "Ce numero ne correspond pas aux formats connus Chronopost, Colissimo ou Mondial Relay.";
  private static final String MONDIAL_RELAY_COMPLETION_LABEL =
      "Code postal destinataire requis pour activer le suivi Mondial Relay";
  private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile("^\\d{5}$");
  private static final Logger log = LoggerFactory.getLogger(DeliveryTrackingService.class);

  private final ParcelRepository parcelRepository;
  private final ParcelEventRepository parcelEventRepository;
  private final MailTrackingCandidateRepository mailTrackingCandidateRepository;
  private final UserRepository userRepository;
  private final TrackingAggregatorService trackingAggregatorService;
  private final TrackingParserService trackingParserService;

  public DeliveryTrackingService(
      ParcelRepository parcelRepository,
      ParcelEventRepository parcelEventRepository,
      MailTrackingCandidateRepository mailTrackingCandidateRepository,
      UserRepository userRepository,
      TrackingAggregatorService trackingAggregatorService,
      TrackingParserService trackingParserService
  ) {
    this.parcelRepository = parcelRepository;
    this.parcelEventRepository = parcelEventRepository;
    this.mailTrackingCandidateRepository = mailTrackingCandidateRepository;
    this.userRepository = userRepository;
    this.trackingAggregatorService = trackingAggregatorService;
    this.trackingParserService = trackingParserService;
  }

  @Transactional(readOnly = true)
  public List<ParcelResponse> listForUser(Long userId) {
    List<Parcel> parcels = parcelRepository.findByUser_IdOrderByUpdatedAtDesc(userId);
    if (parcels.isEmpty()) return List.of();

    List<Long> parcelIds = parcels.stream().map(Parcel::getId).toList();
    Map<Long, List<ParcelEvent>> eventsByParcelId = new HashMap<>();
    for (ParcelEvent event : parcelEventRepository.findByParcel_IdInOrderByParcel_IdAscEventTimeDesc(parcelIds)) {
      Long parcelId = event.getParcel().getId();
      eventsByParcelId.computeIfAbsent(parcelId, ignored -> new ArrayList<>()).add(event);
    }

    return parcels.stream()
        .map(parcel -> ParcelResponse.fromEntity(parcel, eventsByParcelId.getOrDefault(parcel.getId(), List.of())))
        .toList();
  }

  @Transactional(readOnly = true)
  public ParcelResponse getForUser(Long userId, Long parcelId) {
    Parcel parcel = parcelRepository.findByIdAndUser_Id(parcelId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis introuvable"));
    return ParcelResponse.fromEntity(parcel, parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(parcelId));
  }

  @Transactional
  public List<ParcelResponse> refreshAllForUser(Long userId) {
    List<Parcel> existingParcels = parcelRepository.findByUser_IdOrderByUpdatedAtDesc(userId);
    if (existingParcels.isEmpty()) {
      return List.of();
    }

    for (Parcel parcel : existingParcels) {
      if (requiresManualCompletion(parcel)) {
        continue;
      }
      trackingAggregatorService.refreshTracking(parcel);
    }

    return listForUser(userId);
  }

  @Transactional(readOnly = true)
  public List<TrackingCandidateResponse> listCandidatesForReview(Long userId) {
    return mailTrackingCandidateRepository
        .findByUser_IdAndStatusOrderByReceivedAtDescCreatedAtDesc(userId, TrackingCandidateStatus.NEEDS_REVIEW)
        .stream()
        .map(TrackingCandidateResponse::fromEntity)
        .toList();
  }

  @Transactional
  public ParcelResponse createManual(Long userId, ParcelCreateRequest request) {
    String rawTrackingNumber = request == null ? null : request.trackingNumber();
    if (!trackingParserService.isValidTrackingNumber(rawTrackingNumber)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Numero de suivi invalide");
    }

    String normalizedTrackingNumber = trackingParserService.normalizeTrackingNumber(rawTrackingNumber);
    String carrierSlug = resolveManualCarrier(request == null ? null : request.carrierSlug(), normalizedTrackingNumber);
    if (carrierSlug == null || carrierSlug.isBlank() || UNKNOWN_CARRIER.equals(carrierSlug)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, SUPPORTED_TRACKING_MESSAGE);
    }
    log.info("Delivery manual tracking normalized={} carrier={}", normalizedTrackingNumber, carrierSlug);
    var existingParcel = parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        userId,
        normalizedTrackingNumber,
        carrierSlug
    );
    TrackingCandidate manualCandidate = manualCandidate(rawTrackingNumber, normalizedTrackingNumber, carrierSlug, request);
    Parcel parcel = existingParcel.orElseGet(() -> createParcel(
            userId,
            null,
            manualCandidate,
            carrierSlug,
            null
        )
    );

    if (existingParcel.isPresent()) {
      String normalizedPostalCode = normalizePostalCode(request == null ? null : request.postalCode());
      if (requiresManualCompletion(parcel) && normalizedPostalCode != null) {
        return completeParcel(userId, parcel.getId(), new ParcelCompletionRequest(normalizedPostalCode));
      }
      if (!requiresManualCompletion(parcel)) {
        trackingAggregatorService.refreshTracking(parcel);
      }
    }
    return ParcelResponse.fromEntity(parcel, parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(parcel.getId()));
  }

  @Transactional
  public ParcelResponse completeParcel(Long userId, Long parcelId, ParcelCompletionRequest request) {
    Parcel parcel = parcelRepository.findByIdAndUser_Id(parcelId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis introuvable"));

    if (!"mondial-relay".equals(TrackingCarrierRules.normalizeCarrierSlug(parcel.getCarrierSlug()))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seuls les suivis Mondial Relay peuvent etre completes ici.");
    }

    String postalCode = normalizePostalCode(request == null ? null : request.postalCode());
    if (postalCode == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Renseigne un code postal francais sur 5 chiffres.");
    }

    Map<String, Object> payload = new HashMap<>(parcel.getRawCurrentPayload() == null ? Map.of() : parcel.getRawCurrentPayload());
    payload.put("mondial_relay_postal_code", postalCode);
    payload.put("destination_postal_code", postalCode);
    payload.put("tracking_url", buildMondialRelayTrackingUrl(parcel.getNormalizedTrackingNumber(), postalCode));
    parcel.setRawCurrentPayload(payload);
    if (parcel.getStatus() == null || parcel.getStatus() == ParcelStatus.INCOMPLETE) {
      parcel.setStatus(ParcelStatus.PENDING);
    }
    parcel.setStatusLabel("Suivi Mondial Relay complete, rafraichissement en cours");

    Parcel saved = parcelRepository.save(parcel);
    Parcel refreshed = trackingAggregatorService.refreshTracking(saved);
    return ParcelResponse.fromEntity(
        refreshed,
        parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(refreshed.getId())
    );
  }

  @Transactional
  public ParcelResponse refreshForUser(Long userId, Long parcelId) {
    Parcel parcel = parcelRepository.findByIdAndUser_Id(parcelId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis introuvable"));
    if (requiresManualCompletion(parcel)) {
      return ParcelResponse.fromEntity(parcel, parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(parcelId));
    }
    Parcel refreshed = trackingAggregatorService.refreshTracking(parcel);
    return ParcelResponse.fromEntity(
        refreshed,
        parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(refreshed.getId())
    );
  }

  @Transactional
  public void deleteForUser(Long userId, Long parcelId) {
    Parcel parcel = parcelRepository.findByIdAndUser_Id(parcelId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis introuvable"));
    parcelRepository.delete(parcel);
  }

  @Transactional
  public ParcelResponse confirmCandidate(Long userId, Long candidateId) {
    MailTrackingCandidate storedCandidate = mailTrackingCandidateRepository.findByIdAndUser_Id(candidateId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidat introuvable"));

    TrackingCandidate candidate = new TrackingCandidate(
        storedCandidate.getCarrierSlug(),
        storedCandidate.getTrackingNumber(),
        storedCandidate.getNormalizedTrackingNumber(),
        storedCandidate.getConfidenceScore(),
        TrackingParserService.TrackingConfidence.HIGH,
        storedCandidate.getTrackingUrl(),
        storedCandidate.getContextSnippet(),
        storedCandidate.getMerchantName(),
        storedCandidate.getRawStatus(),
        storedCandidate.getReason()
    );

    String carrierSlug = normalizeCarrier(candidate.carrierSlug(), candidate.normalizedTrackingNumber());
    boolean alreadyExists = parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        userId,
        candidate.normalizedTrackingNumber(),
        carrierSlug
    ).isPresent();
    Parcel parcel = upsertFromMail(userId, storedCandidate.getMailAccount(), candidate, storedCandidate.getProviderMessageId());
    applyMailStatusHint(parcel, candidate, storedCandidate.getReceivedAt());
    storedCandidate.setParcel(parcel);
    storedCandidate.setStatus(alreadyExists ? TrackingCandidateStatus.DUPLICATE : TrackingCandidateStatus.CONFIRMED);
    mailTrackingCandidateRepository.save(storedCandidate);

    return ParcelResponse.fromEntity(parcel, parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(parcel.getId()));
  }

  @Transactional
  public TrackingCandidateResponse ignoreCandidate(Long userId, Long candidateId) {
    MailTrackingCandidate storedCandidate = mailTrackingCandidateRepository.findByIdAndUser_Id(candidateId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidat introuvable"));
    storedCandidate.setStatus(TrackingCandidateStatus.IGNORED);
    return TrackingCandidateResponse.fromEntity(mailTrackingCandidateRepository.save(storedCandidate));
  }

  @Transactional
  public Parcel upsertFromMail(
      Long userId,
      MailAccount mailAccount,
      TrackingCandidate candidate,
      String sourceProviderMessageId
  ) {
    String carrierSlug = normalizeCarrier(candidate.carrierSlug(), candidate.normalizedTrackingNumber());
    return parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
            userId,
            candidate.normalizedTrackingNumber(),
            carrierSlug
        )
        .orElseGet(() -> createParcel(userId, mailAccount, candidate, carrierSlug, sourceProviderMessageId));
  }

  @Transactional
  public MailCandidateImportResult importDetectedFromMail(
      Long userId,
      MailAccount mailAccount,
      TrackingCandidate candidate,
      String sourceProviderMessageId,
      String sourceSubject,
      String sourceSender,
      OffsetDateTime receivedAt
  ) {
    String carrierSlug = normalizeCarrier(candidate.carrierSlug(), candidate.normalizedTrackingNumber());
    boolean alreadyExists = parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        userId,
        candidate.normalizedTrackingNumber(),
        carrierSlug
    ).isPresent();
    Parcel parcel = upsertFromMail(userId, mailAccount, candidate, sourceProviderMessageId);
    applyMailStatusHint(parcel, candidate, receivedAt);
    saveMailCandidate(
        userId,
        mailAccount,
        candidate,
        sourceProviderMessageId,
        sourceSubject,
        sourceSender,
        receivedAt,
        alreadyExists ? TrackingCandidateStatus.DUPLICATE : TrackingCandidateStatus.DETECTED,
        parcel
    );
    return new MailCandidateImportResult(parcel, !alreadyExists);
  }

  @Transactional
  public TrackingCandidateResponse storeCandidateForReview(
      Long userId,
      MailAccount mailAccount,
      TrackingCandidate candidate,
      String sourceProviderMessageId,
      String sourceSubject,
      String sourceSender,
      OffsetDateTime receivedAt
  ) {
    MailTrackingCandidate stored = saveMailCandidate(
        userId,
        mailAccount,
        candidate,
        sourceProviderMessageId,
        sourceSubject,
        sourceSender,
        receivedAt,
        TrackingCandidateStatus.NEEDS_REVIEW,
        null
    );
    return TrackingCandidateResponse.fromEntity(stored);
  }

  private Parcel createParcel(
      Long userId,
      MailAccount mailAccount,
      TrackingCandidate candidate,
      String carrierSlug,
      String sourceProviderMessageId
  ) {
    User user = findUser(userId);

    Parcel parcel = new Parcel();
    parcel.setUser(user);
    parcel.setMailAccount(mailAccount);
    parcel.setTrackingNumber(candidate.trackingNumber());
    parcel.setNormalizedTrackingNumber(candidate.normalizedTrackingNumber());
    parcel.setCarrierSlug(carrierSlug);
    parcel.setAggregator(DirectCarrierTrackingService.PROVIDER);
    boolean requiresCompletion = requiresManualCompletion(carrierSlug, candidate.trackingUrl());
    ParcelStatus hintedStatus = rawStatusToParcelStatus(candidate.rawStatus());
    parcel.setStatus(requiresCompletion ? ParcelStatus.INCOMPLETE : hintedStatus == null ? ParcelStatus.PENDING : hintedStatus);
    parcel.setStatusLabel(requiresCompletion ? MONDIAL_RELAY_COMPLETION_LABEL : mailStatusLabel(candidate.rawStatus()));
    if (!requiresCompletion && hintedStatus == ParcelStatus.DELIVERED) {
      parcel.setDeliveredAt(OffsetDateTime.now(ZoneOffset.UTC));
    }
    parcel.setSourceProviderMessageId(sourceProviderMessageId);
    parcel.setFirstSeenAt(OffsetDateTime.now(ZoneOffset.UTC));
    parcel.setRawCurrentPayload(mailCandidatePayload(candidate));

    try {
      Parcel saved = parcelRepository.saveAndFlush(parcel);
      if (!requiresCompletion) {
        trackingAggregatorService.registerTracking(saved);
      }
      return saved;
    } catch (DataIntegrityViolationException ex) {
      return parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
              userId,
              candidate.normalizedTrackingNumber(),
              carrierSlug
          )
          .orElseThrow(() -> ex);
    }
  }

  private MailTrackingCandidate saveMailCandidate(
      Long userId,
      MailAccount mailAccount,
      TrackingCandidate candidate,
      String sourceProviderMessageId,
      String sourceSubject,
      String sourceSender,
      OffsetDateTime receivedAt,
      TrackingCandidateStatus status,
      Parcel parcel
  ) {
    User user = findUser(userId);
    String carrierSlug = normalizeCarrier(candidate.carrierSlug(), candidate.normalizedTrackingNumber());
    String dedupeKey = candidateDedupeKey(mailAccount, sourceProviderMessageId, candidate, carrierSlug);

    MailTrackingCandidate stored = mailTrackingCandidateRepository.findByUser_IdAndDedupeKey(userId, dedupeKey)
        .orElseGet(MailTrackingCandidate::new);
    stored.setUser(user);
    stored.setMailAccount(mailAccount);
    stored.setParcel(parcel == null ? stored.getParcel() : parcel);
    stored.setDedupeKey(dedupeKey);
    stored.setProviderMessageId(limit(sourceProviderMessageId, 255));
    stored.setSourceSender(limit(sourceSender, 500));
    stored.setSourceSubject(limit(sourceSubject, 500));
    stored.setReceivedAt(receivedAt);
    stored.setTrackingNumber(limit(candidate.trackingNumber(), 80));
    stored.setNormalizedTrackingNumber(limit(candidate.normalizedTrackingNumber(), 80));
    stored.setCarrierSlug(limit(carrierSlug, 80));
    stored.setTrackingUrl(candidate.trackingUrl());
    stored.setMerchantName(limit(candidate.merchantName(), 255));
    stored.setRawStatus(limit(candidate.rawStatus(), 120));
    stored.setContextSnippet(limit(candidate.contextSnippet(), 700));
    stored.setConfidenceScore(candidate.confidenceScore());
    stored.setConfidenceLevel(candidate.confidenceLevel() == null ? "UNKNOWN" : candidate.confidenceLevel().name());
    stored.setStatus(status);
    stored.setReason(limit(candidate.reason(), 700));

    try {
      return mailTrackingCandidateRepository.saveAndFlush(stored);
    } catch (DataIntegrityViolationException ex) {
      return mailTrackingCandidateRepository.findByUser_IdAndDedupeKey(userId, dedupeKey)
          .orElseThrow(() -> ex);
    }
  }

  private Map<String, Object> mailCandidatePayload(TrackingCandidate candidate) {
    Map<String, Object> payload = new HashMap<>();
    putIfPresent(payload, "tracking_url", candidate.trackingUrl());
    if (requiresManualCompletion(candidate.carrierSlug(), candidate.trackingUrl())) {
      payload.put("tracking_completion_required", "MONDIAL_RELAY_POSTAL_CODE");
    } else {
      String postalCode = extractMondialRelayPostalCode(candidate.trackingUrl());
      putIfPresent(payload, "mondial_relay_postal_code", postalCode);
      putIfPresent(payload, "destination_postal_code", postalCode);
    }
    putIfPresent(payload, "source_context_snippet", candidate.contextSnippet());
    putIfPresent(payload, "source_merchant_name", candidate.merchantName());
    putIfPresent(payload, "source_raw_status", candidate.rawStatus());
    payload.put("detection_confidence_score", candidate.confidenceScore());
    if (candidate.confidenceLevel() != null) {
      payload.put("detection_confidence_level", candidate.confidenceLevel().name());
    }
    putIfPresent(payload, "detection_reason", candidate.reason());
    return payload;
  }

  private User findUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
  }

  private void applyMailStatusHint(Parcel parcel, TrackingCandidate candidate, OffsetDateTime receivedAt) {
    if (parcel == null || candidate == null) {
      return;
    }
    if (requiresManualCompletion(parcel)) {
      return;
    }
    ParcelStatus hintedStatus = rawStatusToParcelStatus(candidate.rawStatus());
    if (hintedStatus == null) {
      return;
    }
    ParcelStatus currentStatus = parcel.getStatus();
    if (statusPriority(hintedStatus) < statusPriority(currentStatus)) {
      return;
    }
    if (hintedStatus == currentStatus && hintedStatus != ParcelStatus.DELIVERED) {
      return;
    }

    parcel.setStatus(hintedStatus);
    parcel.setStatusLabel(mailStatusLabel(candidate.rawStatus()));
    if (hintedStatus == ParcelStatus.DELIVERED && parcel.getDeliveredAt() == null) {
      parcel.setDeliveredAt(receivedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : receivedAt);
    }
    parcelRepository.save(parcel);
  }

  private ParcelStatus rawStatusToParcelStatus(String rawStatus) {
    if (rawStatus == null || rawStatus.isBlank()) {
      return null;
    }
    return switch (rawStatus.trim().toUpperCase()) {
      case "DELIVERED" -> ParcelStatus.DELIVERED;
      case "OUT_FOR_DELIVERY" -> ParcelStatus.OUT_FOR_DELIVERY;
      case "IN_TRANSIT", "SHIPPED" -> ParcelStatus.IN_TRANSIT;
      default -> null;
    };
  }

  private String mailStatusLabel(String rawStatus) {
    ParcelStatus status = rawStatusToParcelStatus(rawStatus);
    if (status == null) {
      return "Suivi detecte";
    }
    return switch (status) {
      case DELIVERED -> "Livre selon email transporteur";
      case OUT_FOR_DELIVERY -> "En cours de livraison selon email transporteur";
      case IN_TRANSIT -> "En transit selon email transporteur";
      default -> "Suivi detecte";
    };
  }

  private int statusPriority(ParcelStatus status) {
    if (status == null) {
      return 0;
    }
    return switch (status) {
      case INCOMPLETE, PENDING, UNKNOWN -> 0;
      case REGISTERED -> 1;
      case IN_TRANSIT -> 2;
      case OUT_FOR_DELIVERY -> 3;
      case DELIVERED -> 4;
      case EXCEPTION -> 5;
    };
  }

  private String candidateDedupeKey(
      MailAccount mailAccount,
      String sourceProviderMessageId,
      TrackingCandidate candidate,
      String carrierSlug
  ) {
    String accountPart = mailAccount == null || mailAccount.getId() == null ? "manual" : String.valueOf(mailAccount.getId());
    String messagePart = sourceProviderMessageId == null || sourceProviderMessageId.isBlank()
        ? "no-message"
        : sourceProviderMessageId.trim();
    return accountPart + "|" + messagePart + "|" + candidate.normalizedTrackingNumber() + "|" + carrierSlug;
  }

  private void putIfPresent(Map<String, Object> payload, String key, String value) {
    if (value != null && !value.isBlank()) {
      payload.put(key, value);
    }
  }

  private String limit(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return null;
    }
    return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
  }

  private String normalizeCarrier(String carrierSlug, String normalizedTrackingNumber) {
    if (carrierSlug == null || carrierSlug.isBlank()) {
      String inferredCarrier = trackingParserService.inferCarrierSlug(normalizedTrackingNumber);
      return inferredCarrier == null ? UNKNOWN_CARRIER : inferredCarrier;
    }
    String normalizedCarrier = carrierSlug.trim().toLowerCase();
    return switch (normalizedCarrier) {
      case "la poste", "la-poste", "laposte" -> "laposte";
      case "mondialrelay", "mondial relay" -> "mondial-relay";
      case "amazon", "amazon logistics" -> "amazon-logistics";
      default -> normalizedCarrier;
    };
  }

  private String resolveManualCarrier(String requestedCarrier, String normalizedTrackingNumber) {
    String normalizedCarrier = TrackingCarrierRules.normalizeCarrierSlug(requestedCarrier);
    if (normalizedCarrier != null) {
      if (TrackingCarrierRules.isSupportedCarrier(normalizedCarrier)
          && !TrackingCarrierRules.isValidForCarrier(normalizedTrackingNumber, normalizedCarrier)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            invalidCarrierFormatMessage(normalizedCarrier)
        );
      }
      return normalizeCarrier(normalizedCarrier, normalizedTrackingNumber);
    }

    String supportedCarrier = TrackingCarrierRules.inferSupportedCarrier(normalizedTrackingNumber);
    if (supportedCarrier != null) {
      return supportedCarrier;
    }
    return null;
  }

  private String invalidCarrierFormatMessage(String carrierSlug) {
    return switch (TrackingCarrierRules.normalizeCarrierSlug(carrierSlug)) {
      case "chronopost" -> "Ce numero ne correspond pas a un format Chronopost reconnu.";
      case "colissimo" -> "Ce numero ne correspond pas a un format Colissimo / La Poste reconnu.";
      case "mondial-relay" -> "Ce numero ne correspond pas a un format Mondial Relay reconnu.";
      default -> SUPPORTED_TRACKING_MESSAGE;
    };
  }

  private TrackingCandidate manualCandidate(
      String rawTrackingNumber,
      String normalizedTrackingNumber,
      String carrierSlug,
      ParcelCreateRequest request
  ) {
    String normalizedPostalCode = normalizePostalCode(request == null ? null : request.postalCode());
    String trackingUrl = requiresManualCompletion(carrierSlug, null) && normalizedPostalCode != null
        ? buildMondialRelayTrackingUrl(normalizedTrackingNumber, normalizedPostalCode)
        : null;
    return new TrackingCandidate(
        carrierSlug,
        rawTrackingNumber.trim(),
        normalizedTrackingNumber,
        100,
        TrackingParserService.TrackingConfidence.HIGH,
        trackingUrl,
        null,
        null,
        null,
        "manual parcel"
    );
  }

  private boolean requiresManualCompletion(Parcel parcel) {
    if (parcel == null) {
      return false;
    }
    String trackingUrl = null;
    if (parcel.getRawCurrentPayload() != null) {
      Object value = parcel.getRawCurrentPayload().get("tracking_url");
      trackingUrl = value == null ? null : String.valueOf(value);
    }
    return requiresManualCompletion(parcel.getCarrierSlug(), trackingUrl);
  }

  private boolean requiresManualCompletion(String carrierSlug, String trackingUrl) {
    return "mondial-relay".equals(TrackingCarrierRules.normalizeCarrierSlug(carrierSlug))
        && extractMondialRelayPostalCode(trackingUrl) == null;
  }

  private String normalizePostalCode(String postalCode) {
    if (postalCode == null) {
      return null;
    }
    String normalized = postalCode.replaceAll("\\D", "");
    return POSTAL_CODE_PATTERN.matcher(normalized).matches() ? normalized : null;
  }

  private String extractMondialRelayPostalCode(String trackingUrl) {
    if (trackingUrl == null || trackingUrl.isBlank()) {
      return null;
    }
    java.util.regex.Matcher matcher = Pattern.compile("[?&]codepostal=(\\d{5})(?:[&#]|$)", Pattern.CASE_INSENSITIVE)
        .matcher(trackingUrl);
    return matcher.find() ? matcher.group(1) : null;
  }

  private String buildMondialRelayTrackingUrl(String trackingNumber, String postalCode) {
    return "https://www.mondialrelay.fr/suivi-de-colis/?numeroExpedition="
        + java.net.URLEncoder.encode(trackingNumber.trim(), java.nio.charset.StandardCharsets.UTF_8)
        + "&codePostal="
        + java.net.URLEncoder.encode(postalCode.trim(), java.nio.charset.StandardCharsets.UTF_8);
  }

  public record MailCandidateImportResult(Parcel parcel, boolean imported) {
  }
}
