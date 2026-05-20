package backend.controller;

import backend.dto.ParcelResponse;
import backend.dto.TrackingCandidateResponse;
import backend.entity.User;
import backend.service.DeliveryTrackingService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/delivery/tracking-candidates")
public class DeliveryTrackingCandidateController {

  private final DeliveryTrackingService deliveryTrackingService;

  public DeliveryTrackingCandidateController(DeliveryTrackingService deliveryTrackingService) {
    this.deliveryTrackingService = deliveryTrackingService;
  }

  @GetMapping
  public List<TrackingCandidateResponse> list(@AuthenticationPrincipal User currentUser) {
    return deliveryTrackingService.listCandidatesForReview(currentUser.getId());
  }

  @PostMapping("/{id}/confirm")
  public ParcelResponse confirm(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
    return deliveryTrackingService.confirmCandidate(currentUser.getId(), id);
  }

  @PostMapping("/{id}/ignore")
  public TrackingCandidateResponse ignore(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
    return deliveryTrackingService.ignoreCandidate(currentUser.getId(), id);
  }
}
