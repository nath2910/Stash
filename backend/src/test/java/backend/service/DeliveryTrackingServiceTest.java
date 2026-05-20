package backend.service;

import backend.dto.ParcelCreateRequest;
import backend.entity.MailAccount;
import backend.entity.Parcel;
import backend.entity.User;
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

    var response = service.createManual(1L, new ParcelCreateRequest("LA-123456789-FR", "colissimo"));

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

    var response = service.createManual(1L, new ParcelCreateRequest("LA123456789FR", "colissimo"));

    Assertions.assertEquals(31L, response.id());
    Mockito.verify(trackingAggregatorService).refreshTracking(existing);
    Mockito.verify(trackingAggregatorService, Mockito.never()).registerTracking(Mockito.any(Parcel.class));
  }

  @Test
  void deleteRemovesOnlyUserParcel() {
    Parcel existing = new Parcel();
    existing.setId(41L);
    Mockito.when(parcelRepository.findByIdAndUser_Id(41L, 1L)).thenReturn(Optional.of(existing));

    service.deleteForUser(1L, 41L);

    Mockito.verify(parcelRepository).delete(existing);
  }
}
