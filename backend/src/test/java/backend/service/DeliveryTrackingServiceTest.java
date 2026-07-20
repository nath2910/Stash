package backend.service;

import backend.dto.ParcelCompletionRequest;
import backend.dto.ParcelCreateRequest;
import backend.entity.MailAccount;
import backend.entity.MailTrackingCandidate;
import backend.entity.Parcel;
import backend.entity.ParcelStatus;
import backend.entity.User;
import java.util.HashMap;
import backend.repository.MailTrackingCandidateRepository;
import backend.repository.ParcelEventRepository;
import backend.repository.ParcelRepository;
import backend.repository.UserRepository;
import backend.service.TrackingParserService.TrackingCandidate;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

class DeliveryTrackingServiceTest {

  @Mock
  private ParcelRepository parcelRepository;

  @Mock
  private ParcelEventRepository parcelEventRepository;

  @Mock
  private MailTrackingCandidateRepository mailTrackingCandidateRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private TrackingAggregatorService trackingAggregatorService;

  private DeliveryTrackingService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
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
  void upsertCreatesParcelOnlyOnceForTrackingKey() {
    TrackingCandidate candidate = new TrackingCandidate("ups", "1Z999AA10123456784", "1Z999AA10123456784");
    User user = Mockito.mock(User.class);
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "1Z999AA10123456784",
        "ups"
    )).thenReturn(Optional.empty());
    Mockito.when(parcelRepository.saveAndFlush(Mockito.any(Parcel.class))).thenAnswer(invocation -> {
      Parcel parcel = invocation.getArgument(0);
      parcel.setId(10L);
      return parcel;
    });

    Parcel created = service.upsertFromMail(
        1L,
        new MailAccount(),
        candidate,
        "gmail-message-1"
    );

    Assertions.assertEquals("1Z999AA10123456784", created.getNormalizedTrackingNumber());
    Mockito.verify(parcelRepository).saveAndFlush(Mockito.any(Parcel.class));
    Mockito.verify(trackingAggregatorService).registerTracking(created);
  }

  @Test
  void upsertReturnsExistingParcelWithoutRegisteringAgain() {
    TrackingCandidate candidate = new TrackingCandidate("ups", "1Z999AA10123456784", "1Z999AA10123456784");
    Parcel existing = new Parcel();
    existing.setId(20L);
    existing.setNormalizedTrackingNumber("1Z999AA10123456784");

    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "1Z999AA10123456784",
        "ups"
    )).thenReturn(Optional.of(existing));

    Parcel result = service.upsertFromMail(1L, new MailAccount(), candidate, "gmail-message-1");

    Assertions.assertSame(existing, result);
    Mockito.verify(parcelRepository, Mockito.never()).saveAndFlush(Mockito.any(Parcel.class));
    Mockito.verifyNoInteractions(trackingAggregatorService);
  }

  @Test
  void createManualParcelNormalizesAndRefreshesTracking() {
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
    Mockito.when(trackingAggregatorService.refreshTracking(Mockito.any(Parcel.class))).thenAnswer(invocation ->
        invocation.getArgument(0)
    );

    var response = service.createManual(1L, new ParcelCreateRequest("LA-123456789-FR", "colissimo", null));

    Assertions.assertEquals("LA123456789FR", response.normalizedTrackingNumber());
    Assertions.assertEquals("colissimo", response.carrierSlug());
    Mockito.verify(trackingAggregatorService).registerTracking(Mockito.any(Parcel.class));
    Mockito.verify(trackingAggregatorService, Mockito.never()).refreshTracking(Mockito.any(Parcel.class));
  }

  @Test
  void createManualRefreshesExistingParcelWithoutRegisteringAgain() {
    Parcel existing = new Parcel();
    existing.setId(31L);
    existing.setTrackingNumber("LA123456789FR");
    existing.setNormalizedTrackingNumber("LA123456789FR");
    existing.setCarrierSlug("colissimo");
    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "LA123456789FR",
        "colissimo"
    )).thenReturn(Optional.of(existing));
    Mockito.when(trackingAggregatorService.refreshTracking(existing)).thenReturn(existing);

    var response = service.createManual(1L, new ParcelCreateRequest("LA123456789FR", "colissimo", null));

    Assertions.assertEquals(31L, response.id());
    Mockito.verify(trackingAggregatorService).refreshTracking(existing);
    Mockito.verify(trackingAggregatorService, Mockito.never()).registerTracking(Mockito.any(Parcel.class));
  }

  @Test
  void createManualMondialRelayWithoutPostalCodeCreatesIncompleteParcel() {
    User user = Mockito.mock(User.class);
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "12345678",
        "mondial-relay"
    )).thenReturn(Optional.empty());
    Mockito.when(parcelRepository.saveAndFlush(Mockito.any(Parcel.class))).thenAnswer(invocation -> {
      Parcel parcel = invocation.getArgument(0);
      parcel.setId(32L);
      return parcel;
    });

    var response = service.createManual(1L, new ParcelCreateRequest("12 34 56 78", null, null));

    Assertions.assertEquals("12345678", response.normalizedTrackingNumber());
    Assertions.assertEquals("mondial-relay", response.carrierSlug());
    Assertions.assertEquals(ParcelStatus.INCOMPLETE, response.status());
    Mockito.verify(trackingAggregatorService, Mockito.never()).registerTracking(Mockito.any(Parcel.class));
  }

  @Test
  void createManualMondialRelayWithPostalCodeRegistersImmediately() {
    User user = Mockito.mock(User.class);
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "12345678",
        "mondial-relay"
    )).thenReturn(Optional.empty());
    Mockito.when(parcelRepository.saveAndFlush(Mockito.any(Parcel.class))).thenAnswer(invocation -> {
      Parcel parcel = invocation.getArgument(0);
      parcel.setId(34L);
      return parcel;
    });

    var response = service.createManual(1L, new ParcelCreateRequest("12 34 56 78", null, "17000"));

    Assertions.assertEquals(ParcelStatus.PENDING, response.status());
    Mockito.verify(trackingAggregatorService).registerTracking(Mockito.any(Parcel.class));
  }

  @Test
  void createManualAutoDetectsChronopostFrPrefixWithoutFallingBackToColissimo() {
    User user = Mockito.mock(User.class);
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "XY123456789FR",
        "chronopost"
    )).thenReturn(Optional.empty());
    Mockito.when(parcelRepository.saveAndFlush(Mockito.any(Parcel.class))).thenAnswer(invocation -> {
      Parcel parcel = invocation.getArgument(0);
      parcel.setId(33L);
      return parcel;
    });

    var response = service.createManual(1L, new ParcelCreateRequest("XY123456789FR", null, null));

    Assertions.assertEquals("chronopost", response.carrierSlug());
  }

  @Test
  void createManualAutoDetectsLaPoste15CharTrackingWithoutFallingBackToChronopost() {
    User user = Mockito.mock(User.class);
    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "05308083313940F",
        "colissimo"
    )).thenReturn(Optional.empty());
    Mockito.when(parcelRepository.saveAndFlush(Mockito.any(Parcel.class))).thenAnswer(invocation -> {
      Parcel parcel = invocation.getArgument(0);
      parcel.setId(35L);
      return parcel;
    });

    var response = service.createManual(1L, new ParcelCreateRequest("05308083313940F", null, null));

    Assertions.assertEquals("colissimo", response.carrierSlug());
    Assertions.assertEquals("05308083313940F", response.normalizedTrackingNumber());
  }

  @Test
  void createManualRejectsCarrierMismatchWithClearMessage() {
    ResponseStatusException exception = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.createManual(1L, new ParcelCreateRequest("LA123456789FR", "mondial-relay", null))
    );

    Assertions.assertEquals(
        "Ce numero ne correspond pas a un format Mondial Relay reconnu.",
        exception.getReason()
    );
  }

  @Test
  void deleteRemovesOnlyUserParcel() {
    Parcel existing = new Parcel();
    existing.setId(41L);
    Mockito.when(parcelRepository.findByIdAndUser_Id(41L, 1L)).thenReturn(Optional.of(existing));

    service.deleteForUser(1L, 41L);

    Mockito.verify(parcelRepository).delete(existing);
  }

  @Test
  void refreshAllRefreshesEveryUserParcel() {
    Parcel first = new Parcel();
    first.setId(51L);
    Parcel second = new Parcel();
    second.setId(52L);
    second.setCarrierSlug("mondial-relay");
    second.setStatus(ParcelStatus.INCOMPLETE);
    second.setRawCurrentPayload(new HashMap<>());
    Mockito.when(parcelRepository.findByUser_IdOrderByUpdatedAtDesc(1L)).thenReturn(java.util.List.of(first, second));
    Mockito.when(trackingAggregatorService.refreshTracking(first)).thenReturn(first);
    Mockito.when(parcelEventRepository.findByParcel_IdInOrderByParcel_IdAscEventTimeDesc(java.util.List.of(51L, 52L)))
        .thenReturn(java.util.List.of());

    service.refreshAllForUser(1L);

    Mockito.verify(trackingAggregatorService).refreshTracking(first);
    Mockito.verify(trackingAggregatorService, Mockito.never()).refreshTracking(second);
  }

  @Test
  void completeParcelStoresMondialRelayPostalCodeAndRefreshes() {
    Parcel parcel = new Parcel();
    parcel.setId(71L);
    parcel.setTrackingNumber("12345678");
    parcel.setNormalizedTrackingNumber("12345678");
    parcel.setCarrierSlug("mondial-relay");
    parcel.setStatus(ParcelStatus.INCOMPLETE);
    parcel.setRawCurrentPayload(new HashMap<>());
    Mockito.when(parcelRepository.findByIdAndUser_Id(71L, 1L)).thenReturn(Optional.of(parcel));
    Mockito.when(parcelRepository.save(Mockito.any(Parcel.class))).thenAnswer(invocation -> invocation.getArgument(0));
    Mockito.when(trackingAggregatorService.refreshTracking(parcel)).thenReturn(parcel);

    var response = service.completeParcel(1L, 71L, new ParcelCompletionRequest("17000"));

    Assertions.assertEquals(71L, response.id());
    Assertions.assertEquals("17000", String.valueOf(parcel.getRawCurrentPayload().get("mondial_relay_postal_code")));
    Assertions.assertTrue(String.valueOf(parcel.getRawCurrentPayload().get("tracking_url")).contains("codePostal=17000"));
    Mockito.verify(trackingAggregatorService).refreshTracking(parcel);
  }

  @Test
  void mailImportPromotesExistingParcelToDeliveredWhenCarrierEmailSaysDelivered() {
    Parcel existing = new Parcel();
    existing.setId(61L);
    existing.setStatus(ParcelStatus.OUT_FOR_DELIVERY);
    existing.setTrackingNumber("XY123456789FR");
    existing.setNormalizedTrackingNumber("XY123456789FR");
    existing.setCarrierSlug("chronopost");
    User user = Mockito.mock(User.class);

    Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(parcelRepository.findByUser_IdAndNormalizedTrackingNumberAndCarrierSlug(
        1L,
        "XY123456789FR",
        "chronopost"
    )).thenReturn(Optional.of(existing));
    Mockito.when(parcelRepository.save(Mockito.any(Parcel.class))).thenAnswer(invocation -> invocation.getArgument(0));
    Mockito.when(mailTrackingCandidateRepository.findByUser_IdAndDedupeKey(
        Mockito.eq(1L),
        Mockito.anyString()
    )).thenReturn(Optional.of(new MailTrackingCandidate()));
    Mockito.when(mailTrackingCandidateRepository.saveAndFlush(Mockito.any(MailTrackingCandidate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.importDetectedFromMail(
        1L,
        new MailAccount(),
        new TrackingCandidate(
            "chronopost",
            "XY123456789FR",
            "XY123456789FR",
            90,
            TrackingParserService.TrackingConfidence.HIGH,
            null,
            null,
            "Chronopost",
            "DELIVERED",
            "mail status"
        ),
        "msg-1",
        "Votre colis est livre",
        "Chronopost",
        java.time.OffsetDateTime.parse("2026-07-13T10:00:00Z")
    );

    Assertions.assertEquals(ParcelStatus.DELIVERED, existing.getStatus());
    Assertions.assertEquals("Livre selon email transporteur", existing.getStatusLabel());
    Assertions.assertNotNull(existing.getDeliveredAt());
  }
}
