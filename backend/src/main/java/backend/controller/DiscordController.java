package backend.controller;

import backend.dto.DiscordLinkRequest;
import backend.dto.DiscordEligibilityResponse;
import backend.entity.User;
import backend.service.DiscordAccessService;
import backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/discord")
public class DiscordController {

  private final DiscordAccessService discordService;
  private final UserRepository userRepository;

  public DiscordController(DiscordAccessService discordService, UserRepository userRepository) {
    this.discordService = discordService;
    this.userRepository = userRepository;
  }

  @PostMapping("/link")
  public DiscordEligibilityResponse link(@AuthenticationPrincipal User user, @RequestBody DiscordLinkRequest body) {
    if (body == null || body.discordId() == null || body.discordId().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discordId manquant");
    }
    user.setDiscordId(body.discordId().trim());
    userRepository.save(user);
    boolean eligible = discordService.isEligible(user);
    if (eligible) {
      user.setSubscriptionStatus("active");
      userRepository.save(user);
    }
    return new DiscordEligibilityResponse(eligible, user.getSubscriptionStatus(), eligible ? "Accès accordé via Discord" : "Non éligible");
  }

  @GetMapping("/check")
  public DiscordEligibilityResponse check(@AuthenticationPrincipal User user) {
    if (!discordService.isConfigured()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Discord non configuré côté backend");
    }
    if (user.getDiscordId() == null || user.getDiscordId().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun discordId lié au compte");
    }
    boolean eligible = discordService.isEligible(user);
    if (eligible && !"active".equals(user.getSubscriptionStatus())) {
      user.setSubscriptionStatus("active");
      userRepository.save(user);
    }
    String reason = eligible ? "Accès accordé via Discord" : "Non membre ou rôle manquant";
    return new DiscordEligibilityResponse(eligible, user.getSubscriptionStatus(), reason);
  }
}
