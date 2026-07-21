package backend.service;

import backend.dto.ParcelCreateRequest;
import backend.dto.ParcelResponse;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DeliveryTrackingService {

  private static final String COLISSIMO = "colissimo";
  private static final String RAW_STATUS_AVAILABLE_FOR_PICKUP = "AVAILABLE_FOR_PICKUP";
  private static final String SUPPORTED_TRACKING_MESSAGE =
      "Ce numero ne correspond pas a un format Colissimo / La Poste reconnu.";

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
    List<Parcel> parcels = parcelRepository.findByUser_IdOrderByUpdatedAtDesc(userId).stream()
        .filter(this::isManagedColissimoParcel)
        .toList();
    if (parcels.isEmpty()) {
      return List.of();
    }

    List<Long> parcelIds = parcels.stream().map(Parcel::getId).toList();
    Map<Long, List<ParcelEvent>> eventsByParcelId = new HashMap<>();
    for (ParcelEvent event : parcelEventRepository.findByParcel_IdInOrderByParcel_IdAscEventTimeDesc(parcelIds)) {
      eventsByParcelId.computeIfAbsent(event.getParcel().getId(), ignored -> new ArrayList<>()).add(event);
    }

    return parcels.stream()
        .map(parcel -> ParcelResponse.fromEntity(parcel, eventsByParcelId.getOrDefault(parcel.getId(), List.of())))
        .toList();
  }

  @Transactional(readOnly = true)
  public ParcelResponse getForUser(Long userId, Long parcelId) {
    Parcel parcel = managedParcel(userId, parcelId);
    return ParcelResponse.fromEntity(parcel, parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(parcelId));
  }

  @Transactional
  public List<ParcelResponse> refreshAllForUser(Long userId) {
    List<Parcel> parcels = parcelRepository.findByUser_IdOrderByUpdatedAtDesc(userId).stream()
        .filter(this::isManagedColissimoParcel)
        .filter(this::shouldRefreshParcel)
        .toList();

    for (Parcel parcel : parcels) {
      trackingAggregatorService.refreshTracking(parcel);
    }

    return listForUser(userId);
  }

  @Transactional(readOnly = true)
  public List<TrackingCandidateResponse> listCandidatesForReview(Long userId) {
    return mailTrackingCandidateRepository
        .findByUser_IdAndStatusOrderByReceivedAtDescCreatedAtDesc(userId, TrackingCandidateStatus.NEEDS_REVIEW)
        .stream()
        .filter(candidate -> isColissimoCarrier(candidate.getCarrierSlug()))
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
    var existingParcel = parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        userId,
        normalizedTrackingNumber,
        carrierSlug
    );

    TrackingCandidate manualCandidate = new TrackingCandidate(
        COLISSIMO,
        rawTrackingNumber.trim(),
        normalizedTrackingNumber,
        100,
        TrackingParserService.TrackingConfidence.HIGH,
        TrackingLinkResolver.fallbackTrackingUrl(COLISSIMO, normalizedTrackingNumber),
        null,
        null,
        null,
        "manual parcel"
    );

    Parcel parcel = existingParcel.orElseGet(() -> createParcel(userId, null, manualCandidate, null));
    if (existingParcel.isPresent()) {
      trackingAggregatorService.refreshTracking(parcel);
    }

    return ParcelResponse.fromEntity(parcel, parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(parcel.getId()));
  }

  @Transactional
  public ParcelResponse refreshForUser(Long userId, Long parcelId) {
    Parcel parcel = managedParcel(userId, parcelId);
    Parcel refreshed = trackingAggregatorService.refreshTracking(parcel);
    return ParcelResponse.fromEntity(
        refreshed,
        parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(refreshed.getId())
    );
  }

  @Transactional
  public void deleteForUser(Long userId, Long parcelId) {
    Parcel parcel = managedParcel(userId, parcelId);
    parcelRepository.delete(parcel);
  }

  @Transactional
  public ParcelResponse confirmCandidate(Long userId, Long candidateId) {
    MailTrackingCandidate storedCandidate = mailTrackingCandidateRepository.findByIdAndUser_Id(candidateId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidat introuvable"));
    if (!isColissimoCarrier(storedCandidate.getCarrierSlug())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seuls les suivis Colissimo sont geres.");
    }

    TrackingCandidate candidate = new TrackingCandidate(
        COLISSIMO,
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

    boolean alreadyExists = parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        userId,
        candidate.normalizedTrackingNumber(),
        COLISSIMO
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
    TrackingCandidate colissimoCandidate = requireColissimoCandidate(candidate);
    return parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
            userId,
            colissimoCandidate.normalizedTrackingNumber(),
            COLISSIMO
        )
        .orElseGet(() -> createParcel(userId, mailAccount, colissimoCandidate, sourceProviderMessageId));
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
    TrackingCandidate colissimoCandidate = requireColissimoCandidate(candidate);
    boolean alreadyExists = parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        userId,
        colissimoCandidate.normalizedTrackingNumber(),
        COLISSIMO
    ).isPresent();
    Parcel parcel = upsertFromMail(userId, mailAccount, colissimoCandidate, sourceProviderMessageId);
    applyMailStatusHint(parcel, colissimoCandidate, receivedAt);
    saveMailCandidate(
        userId,
        mailAccount,
        colissimoCandidate,
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
    TrackingCandidate colissimoCandidate = requireColissimoCandidate(candidate);
    MailTrackingCandidate stored = saveMailCandidate(
        userId,
        mailAccount,
        colissimoCandidate,
        sourceProviderMessageId,
        sourceSubject,
        sourceSender,
        receivedAt,
        TrackingCandidateStatus.NEEDS_REVIEW,
        null
    );
    return TrackingCandidateResponse.fromEntity(stored);
  }

  private Parcel managedParcel(Long userId, Long parcelId) {
    Parcel parcel = parcelRepository.findByIdAndUser_Id(parcelId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis introuvable"));
    if (!isManagedColissimoParcel(parcel)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis introuvable");
    }
    return parcel;
  }

  private Parcel createParcel(
      Long userId,
      MailAccount mailAccount,
      TrackingCandidate candidate,
      String sourceProviderMessageId
  ) {
    User user = findUser(userId);

    Parcel parcel = new Parcel();
    parcel.setUser(user);
    parcel.setMailAccount(mailAccount);
    parcel.setTrackingNumber(candidate.trackingNumber());
    parcel.setNormalizedTrackingNumber(candidate.normalizedTrackingNumber());
    parcel.setCarrierSlug(COLISSIMO);
    parcel.setAggregator(DirectCarrierTrackingService.PROVIDER);

    ParcelStatus hintedStatus = rawStatusToParcelStatus(candidate.rawStatus());
    parcel.setStatus(hintedStatus == null ? ParcelStatus.PENDING : hintedStatus);
    parcel.setStatusLabel(mailStatusLabel(candidate.rawStatus()));
    if (hintedStatus == ParcelStatus.DELIVERED) {
      parcel.setDeliveredAt(OffsetDateTime.now(ZoneOffset.UTC));
    }
    parcel.setSourceProviderMessageId(sourceProviderMessageId);
    parcel.setFirstSeenAt(OffsetDateTime.now(ZoneOffset.UTC));
    parcel.setRawCurrentPayload(mailCandidatePayload(candidate));

    try {
      Parcel saved = parcelRepository.saveAndFlush(parcel);
      trackingAggregatorService.registerTracking(saved);
      return parcelRepository.findById(saved.getId()).orElse(saved);
    } catch (DataIntegrityViolationException ex) {
      return parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
              userId,
              candidate.normalizedTrackingNumber(),
              COLISSIMO
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
    String dedupeKey = candidateDedupeKey(mailAccount, sourceProviderMessageId, candidate);

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
    stored.setCarrierSlug(COLISSIMO);
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
    putIfPresent(payload, "tracking_url", TrackingLinkResolver.preferredTrackingUrl(
        candidate.trackingUrl(),
        COLISSIMO,
        candidate.normalizedTrackingNumber()
    ));
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
      case RAW_STATUS_AVAILABLE_FOR_PICKUP, "IN_TRANSIT", "SHIPPED" -> ParcelStatus.IN_TRANSIT;
      default -> null;
    };
  }

  private String mailStatusLabel(String rawStatus) {
    ParcelStatus status = rawStatusToParcelStatus(rawStatus);
    if (status == null) {
      return "Suivi Colissimo detecte";
    }
    return switch (status) {
      case DELIVERED -> "Livre selon email Colissimo";
      case OUT_FOR_DELIVERY -> "En cours de livraison selon email Colissimo";
      case IN_TRANSIT -> RAW_STATUS_AVAILABLE_FOR_PICKUP.equalsIgnoreCase(rawStatus)
          ? "Disponible en relais Pickup"
          : "En transit selon email Colissimo";
      default -> "Suivi Colissimo detecte";
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
      TrackingCandidate candidate
  ) {
    String accountPart = mailAccount == null || mailAccount.getId() == null ? "manual" : String.valueOf(mailAccount.getId());
    String messagePart = sourceProviderMessageId == null || sourceProviderMessageId.isBlank()
        ? "no-message"
        : sourceProviderMessageId.trim();
    return accountPart + "|" + messagePart + "|" + candidate.normalizedTrackingNumber() + "|" + COLISSIMO;
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

  private TrackingCandidate requireColissimoCandidate(TrackingCandidate candidate) {
    if (candidate == null || !isColissimoCarrier(candidate.carrierSlug())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seuls les suivis Colissimo sont geres.");
    }
    return candidate;
  }

  private String resolveManualCarrier(String requestedCarrier, String normalizedTrackingNumber) {
    String normalizedCarrier = TrackingCarrierRules.normalizeCarrierSlug(requestedCarrier);
    if (normalizedCarrier != null && !COLISSIMO.equals(normalizedCarrier)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seuls les suivis Colissimo sont autorises.");
    }
    if (!TrackingCarrierRules.isValidForCarrier(normalizedTrackingNumber, COLISSIMO)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, SUPPORTED_TRACKING_MESSAGE);
    }
    return COLISSIMO;
  }

  private boolean isManagedColissimoParcel(Parcel parcel) {
    return parcel != null && isColissimoCarrier(parcel.getCarrierSlug());
  }

  private boolean shouldRefreshParcel(Parcel parcel) {
    if (parcel == null || !isManagedColissimoParcel(parcel)) {
      return false;
    }
    return switch (parcel.getStatus()) {
      case DELIVERED, INCOMPLETE -> false;
      default -> true;
    };
  }

  private boolean isColissimoCarrier(String carrierSlug) {
    return COLISSIMO.equals(TrackingCarrierRules.normalizeCarrierSlug(carrierSlug));
  }

  public record MailCandidateImportResult(Parcel parcel, boolean imported) {
  }
}
