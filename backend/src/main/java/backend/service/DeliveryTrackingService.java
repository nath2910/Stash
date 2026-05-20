package backend.service;

import backend.dto.ParcelResponse;
import backend.dto.ParcelCreateRequest;
import backend.dto.TrackingCandidateResponse;
import backend.entity.MailAccount;
import backend.entity.MailTrackingCandidate;
import backend.entity.Parcel;
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

  private static final String UNKNOWN_CARRIER = "unknown";

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
    return parcelRepository.findByUser_IdOrderByUpdatedAtDesc(userId).stream()
        .map(parcel -> ParcelResponse.fromEntity(
            parcel,
            parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(parcel.getId())
        ))
        .toList();
  }

  @Transactional(readOnly = true)
  public ParcelResponse getForUser(Long userId, Long parcelId) {
    Parcel parcel = parcelRepository.findByIdAndUser_Id(parcelId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis introuvable"));
    return ParcelResponse.fromEntity(parcel, parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(parcelId));
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
    String carrierSlug = normalizeCarrier(request.carrierSlug(), normalizedTrackingNumber);
    var existingParcel = parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        userId,
        normalizedTrackingNumber,
        carrierSlug
    );
    Parcel parcel = existingParcel.orElseGet(() -> createParcel(
            userId,
            null,
            new TrackingCandidate(carrierSlug, rawTrackingNumber.trim(), normalizedTrackingNumber),
            carrierSlug,
            null
        )
    );

    if (existingParcel.isPresent()) {
      trackingAggregatorService.refreshTracking(parcel);
    }
    return ParcelResponse.fromEntity(parcel, parcelEventRepository.findByParcel_IdOrderByEventTimeDesc(parcel.getId()));
  }

  @Transactional
  public ParcelResponse refreshForUser(Long userId, Long parcelId) {
    Parcel parcel = parcelRepository.findByIdAndUser_Id(parcelId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Colis introuvable"));
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
    parcel.setStatus(ParcelStatus.PENDING);
    parcel.setStatusLabel("Suivi detecte");
    parcel.setSourceProviderMessageId(sourceProviderMessageId);
    parcel.setFirstSeenAt(OffsetDateTime.now(ZoneOffset.UTC));
    parcel.setRawCurrentPayload(mailCandidatePayload(candidate));

    try {
      Parcel saved = parcelRepository.saveAndFlush(parcel);
      trackingAggregatorService.registerTracking(saved);
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

  public record MailCandidateImportResult(Parcel parcel, boolean imported) {
  }
}
