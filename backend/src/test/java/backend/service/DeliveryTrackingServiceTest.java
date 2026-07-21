package backend.service;

import backend.dto.ParcelCreateRequest;
import backend.entity.MailAccount;
import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import backend.entity.User;
import backend.repository.MailTrackingCandidateRepository;
import backend.repository.ParcelEventRepository;
import backend.repository.ParcelRepository;
import backend.repository.UserRepository;
import backend.service.TrackingParserService.TrackingCandidate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

class DeliveryTrackingServiceTest {

  private ParcelRepository parcelRepository;
  private ParcelEventRepository parcelEventRepository;
  private MailTrackingCandidateRepository mailTrackingCandidateRepository;
  private UserRepository userRepository;
  private TrackingAggregatorService trackingAggregatorService;
  private DeliveryTrackingService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    parcelRepository = Mockito.mock(ParcelRepository.class);
    parcelEventRepository = Mockito.mock(ParcelEventRepository.class);
    mailTrackingCandidateRepository = Mockito.mock(MailTrackingCandidateRepository.class);
    userRepository = Mockito.mock(UserRepository.class);
    trackingAggregatorService = Mockito.mock(TrackingAggregatorService.class);
    service = new DeliveryTrackingService(
        parcelRepository,
        parcelEventRepository,
        mailTrackingCandidateRepository,
        userRepository,
        trackingAggregatorService,
        new TrackingParserService()
    );
  }

  @Test
  void upsertCreatesColissimoParcelOnlyOnce() {
    TrackingCandidate candidate = new TrackingCandidate("colissimo", "6A04296519970", "6A04296519970");
    User user = Mockito.mock(User.class);
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "6A04296519970",
        "colissimo"
    )).thenReturn(Optional.empty());
    Mockito.when(parcelRepository.saveAndFlush(Mockito.any(Parcel.class))).thenAnswer(invocation -> {
      Parcel parcel = invocation.getArgument(0);
      parcel.setId(10L);
      return parcel;
    });

    Parcel created = service.upsertFromMail(1L, new MailAccount(), candidate, "gmail-message-1");

    Assertions.assertEquals("6A04296519970", created.getNormalizedTrackingNumber());
    Assertions.assertEquals("colissimo", created.getCarrierSlug());
    Mockito.verify(trackingAggregatorService).registerTracking(created);
  }

  @Test
  void createManualAcceptsOnlyColissimoFormats() {
    User user = Mockito.mock(User.class);
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "LA123456789FR",
        "colissimo"
    )).thenReturn(Optional.empty());
    Mockito.when(parcelRepository.saveAndFlush(Mockito.any(Parcel.class))).thenAnswer(invocation -> {
      Parcel parcel = invocation.getArgument(0);
      parcel.setId(30L);
      return parcel;
    });

    var response = service.createManual(1L, new ParcelCreateRequest("LA-123456789-FR", "colissimo", null));

    Assertions.assertEquals("LA123456789FR", response.normalizedTrackingNumber());
    Assertions.assertEquals("colissimo", response.carrierSlug());
    Mockito.verify(trackingAggregatorService).registerTracking(Mockito.any(Parcel.class));
  }

  @Test
  void createManualRejectsNonColissimoCarrier() {
    ResponseStatusException exception = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.createManual(1L, new ParcelCreateRequest("LA123456789FR", "chronopost", null))
    );

    Assertions.assertEquals("Seuls les suivis Colissimo sont autorises.", exception.getReason());
  }

  @Test
  void refreshAllRefreshesOnlyActiveColissimoParcels() {
    Parcel first = new Parcel();
    first.setId(51L);
    first.setCarrierSlug("colissimo");
    first.setStatus(ParcelStatus.IN_TRANSIT);

    Parcel delivered = new Parcel();
    delivered.setId(52L);
    delivered.setCarrierSlug("colissimo");
    delivered.setStatus(ParcelStatus.DELIVERED);

    Parcel otherCarrier = new Parcel();
    otherCarrier.setId(53L);
    otherCarrier.setCarrierSlug("chronopost");
    otherCarrier.setStatus(ParcelStatus.IN_TRANSIT);
    otherCarrier.setRawCurrentPayload(new HashMap<>());

    Mockito.when(parcelRepository.findByUser_IdOrderByUpdatedAtDesc(1L))
        .thenReturn(List.of(first, delivered, otherCarrier));
    Mockito.when(trackingAggregatorService.refreshTracking(first)).thenReturn(first);
    Mockito.when(parcelEventRepository.findByParcel_IdInOrderByParcel_IdAscEventTimeDesc(List.of(51L, 52L)))
        .thenReturn(List.of());

    service.refreshAllForUser(1L);

    Mockito.verify(trackingAggregatorService).refreshTracking(first);
    Mockito.verify(trackingAggregatorService, Mockito.never()).refreshTracking(delivered);
    Mockito.verify(trackingAggregatorService, Mockito.never()).refreshTracking(otherCarrier);
  }

  @Test
  void importDetectedFromMailPromotesDeliveredStatusFromColissimoMail() {
    Parcel existing = new Parcel();
    existing.setId(61L);
    existing.setStatus(ParcelStatus.OUT_FOR_DELIVERY);
    existing.setTrackingNumber("6A04296519970");
    existing.setNormalizedTrackingNumber("6A04296519970");
    existing.setCarrierSlug("colissimo");
    User user = Mockito.mock(User.class);

    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "6A04296519970",
        "colissimo"
    )).thenReturn(Optional.of(existing));
    Mockito.when(parcelRepository.save(Mockito.any(Parcel.class))).thenAnswer(invocation -> invocation.getArgument(0));
    Mockito.when(mailTrackingCandidateRepository.findByUser_IdAndDedupeKey(
        Mockito.eq(1L),
        Mockito.anyString()
    )).thenReturn(Optional.empty());
    Mockito.when(mailTrackingCandidateRepository.saveAndFlush(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

    TrackingCandidate candidate = new TrackingCandidate(
        "colissimo",
        "6A04296519970",
        "6A04296519970",
        95,
        TrackingParserService.TrackingConfidence.HIGH,
        "https://www.laposte.fr/outils/suivre-vos-envois?code=6A04296519970",
        null,
        "Colissimo",
        "DELIVERED",
        "colissimo"
    );

    service.importDetectedFromMail(
        1L,
        new MailAccount(),
        candidate,
        "gmail-message-1",
        "Votre colis est livre",
        "noreply@laposte.fr",
        java.time.OffsetDateTime.parse("2026-07-19T10:00:00Z")
    );

    Assertions.assertEquals(ParcelStatus.DELIVERED, existing.getStatus());
    Assertions.assertEquals("Livre selon email Colissimo", existing.getStatusLabel());
  }

  @Test
  void importDetectedFromPickupMailUsesPreciseRelayLabel() {
    Parcel existing = new Parcel();
    existing.setId(62L);
    existing.setStatus(ParcelStatus.REGISTERED);
    existing.setTrackingNumber("6Y11138575506");
    existing.setNormalizedTrackingNumber("6Y11138575506");
    existing.setCarrierSlug("colissimo");
    User user = Mockito.mock(User.class);

    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "6Y11138575506",
        "colissimo"
    )).thenReturn(Optional.of(existing));
    Mockito.when(parcelRepository.save(Mockito.any(Parcel.class))).thenAnswer(invocation -> invocation.getArgument(0));
    Mockito.when(mailTrackingCandidateRepository.findByUser_IdAndDedupeKey(
        Mockito.eq(1L),
        Mockito.anyString()
    )).thenReturn(Optional.empty());
    Mockito.when(mailTrackingCandidateRepository.saveAndFlush(Mockito.any())).thenAnswer(invocation -> invocation.getArgument(0));

    TrackingCandidate candidate = new TrackingCandidate(
        "colissimo",
        "6Y11138575506",
        "6Y11138575506",
        85,
        TrackingParserService.TrackingConfidence.HIGH,
        "https://www.laposte.fr/outils/suivre-vos-envois?code=6Y11138575506",
        null,
        "Colissimo",
        "AVAILABLE_FOR_PICKUP",
        "colissimo"
    );

    service.importDetectedFromMail(
        1L,
        new MailAccount(),
        candidate,
        "gmail-message-2",
        "Votre colis AMAZON est arrive en relais Pickup",
        "colissimo@network1.pickup.fr",
        java.time.OffsetDateTime.parse("2026-07-21T09:30:00Z")
    );

    Assertions.assertEquals(ParcelStatus.IN_TRANSIT, existing.getStatus());
    Assertions.assertEquals("Disponible en relais Pickup", existing.getStatusLabel());
  }
}
