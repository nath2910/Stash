package backend.controller;

import backend.dto.LegalProfileResponse;
import backend.dto.LegalProfileUpdateRequest;
import backend.entity.User;
import backend.service.LegalProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

  private final LegalProfileService legalProfileService;

  public UserController(LegalProfileService legalProfileService) {
    this.legalProfileService = legalProfileService;
  }

  @GetMapping("/legal-profile")
  public LegalProfileResponse getLegalProfile(@AuthenticationPrincipal User currentUser) {
    return legalProfileService.getLegalProfile(currentUser.getId());
  }

  @PutMapping("/legal-profile")
  public LegalProfileResponse updateLegalProfile(
      @AuthenticationPrincipal User currentUser,
      @RequestBody LegalProfileUpdateRequest request
  ) {
    return legalProfileService.updateLegalProfile(currentUser.getId(), request);
  }
}
