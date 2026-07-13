package backend.controller;

import backend.dto.ParcelCreateRequest;
import backend.dto.ParcelResponse;
import backend.entity.User;
import backend.service.DeliveryTrackingService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/delivery/parcels")
public class DeliveryTrackingController {

  private final DeliveryTrackingService deliveryTrackingService;

  public DeliveryTrackingController(DeliveryTrackingService deliveryTrackingService) {
    this.deliveryTrackingService = deliveryTrackingService;
  }

  @GetMapping
  public List<ParcelResponse> list(@AuthenticationPrincipal User currentUser) {
    return deliveryTrackingService.listForUser(currentUser.getId());
  }

  @GetMapping("/{id}")
  public ParcelResponse get(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
    return deliveryTrackingService.getForUser(currentUser.getId(), id);
  }

  @PostMapping("/refresh-all")
  public List<ParcelResponse> refreshAll(@AuthenticationPrincipal User currentUser) {
    return deliveryTrackingService.refreshAllForUser(currentUser.getId());
  }

  @PostMapping
  public ParcelResponse create(@AuthenticationPrincipal User currentUser, @RequestBody ParcelCreateRequest request) {
    return deliveryTrackingService.createManual(currentUser.getId(), request);
  }

  @PostMapping("/{id}/refresh")
  public ParcelResponse refresh(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
    return deliveryTrackingService.refreshForUser(currentUser.getId(), id);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@AuthenticationPrincipal User currentUser, @PathVariable Long id) {
    deliveryTrackingService.deleteForUser(currentUser.getId(), id);
  }
}
