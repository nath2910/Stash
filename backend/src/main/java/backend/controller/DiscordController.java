package backend.controller;

import backend.dto.DiscordEligibilityResponse;
import backend.dto.DiscordLinkRequest;
import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.DiscordAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    return new DiscordEligibilityResponse(
        eligible,
        user.getSubscriptionStatus(),
        eligible ? "Eligibilite Discord confirmee" : "Non eligible"
    );
  }

  @GetMapping("/check")
  public DiscordEligibilityResponse check(@AuthenticationPrincipal User user) {
    if (!discordService.isConfigured()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Discord non configure cote backend");
    }
    if (user.getDiscordId() == null || user.getDiscordId().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun discordId lie au compte");
    }

    boolean eligible = discordService.isEligible(user);
    String reason = eligible ? "Eligibilite Discord confirmee" : "Non membre ou role manquant";
    return new DiscordEligibilityResponse(eligible, user.getSubscriptionStatus(), reason);
  }
}
