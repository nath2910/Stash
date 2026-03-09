package backend.service;

import backend.entity.DiscordAllowedGuild;
import backend.entity.User;
import backend.repository.DiscordAllowedGuildRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@Service
public class DiscordAccessService {

  private static final Logger log = LoggerFactory.getLogger(DiscordAccessService.class);

  @Value("${app.discord.bot-token:}")
  private String botToken;

  private final RestTemplate restTemplate = new RestTemplate();
  private final DiscordAllowedGuildRepository guildRepo;

  public DiscordAccessService(DiscordAllowedGuildRepository guildRepo) {
    this.guildRepo = guildRepo;
  }

  public boolean isConfigured() {
    return botToken != null && !botToken.isBlank();
  }

  public boolean isEligible(User user) {
    if (!isConfigured()) {
      log.warn("Discord eligibility: bot token not configured");
      return false;
    }
    String discordId = user.getDiscordId();
    if (discordId == null || discordId.isBlank()) {
      log.debug("Discord eligibility: missing discordId for user {}", user.getId());
      return false;
    }

    List<DiscordAllowedGuild> allowed = guildRepo.findAll();
    if (allowed.isEmpty()) {
      log.warn("Discord eligibility: no allowed guild configured");
      return false;
    }

    for (DiscordAllowedGuild g : allowed) {
      Map<String, Object> member = fetchMember(g.getGuildId(), discordId.trim());
      if (member == null) {
        log.debug("Discord eligibility: user {} not member of guild {}", discordId, g.getGuildId());
        continue; // not in this guild
      }
      if (g.getPremiumRoleId() == null || g.getPremiumRoleId().isBlank()) {
        log.debug("Discord eligibility: user {} accepted (guild {})", discordId, g.getGuildId());
        return true; // membership alone grants access
      }
      Object rolesObj = member.get("roles");
      if (rolesObj instanceof List<?> roles) {
        boolean hasRole = roles.stream().anyMatch(r -> g.getPremiumRoleId().equals(String.valueOf(r)));
        if (hasRole) {
          log.debug("Discord eligibility: user {} accepted (guild {}, role {})", discordId, g.getGuildId(), g.getPremiumRoleId());
          return true;
        } else {
          log.debug("Discord eligibility: user {} missing required role {} in guild {}", discordId, g.getPremiumRoleId(), g.getGuildId());
        }
      }
    }
    log.debug("Discord eligibility: user {} rejected (no matching guild/role)", discordId);
    return false;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> fetchMember(String guildId, String discordId) {
    try {
      String url = "https://discord.com/api/v10/guilds/" + guildId + "/members/" + discordId;
      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", "Bot " + botToken);
      headers.set("User-Agent", "sneakers-app/1.0");
      ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
      if (resp.getStatusCode().is2xxSuccessful()) {
        return resp.getBody();
      }
      log.warn("Discord eligibility: member fetch non-2xx guild={} user={} status={}", guildId, discordId, resp.getStatusCode());
    } catch (Exception e) {
      log.warn("Discord eligibility: error fetching member guild={} user={}: {}", guildId, discordId, e.getMessage());
    }
    return null;
  }
}
