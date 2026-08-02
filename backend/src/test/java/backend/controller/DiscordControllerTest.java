package backend.controller;

import backend.dto.DiscordEligibilityResponse;
import backend.dto.DiscordLinkRequest;
import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.DiscordAccessService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DiscordControllerTest {

  private DiscordAccessService discordAccessService;
  private UserRepository userRepository;
  private DiscordController controller;
  private User user;

  @BeforeEach
  void setUp() {
    discordAccessService = Mockito.mock(DiscordAccessService.class);
    userRepository = Mockito.mock(UserRepository.class);
    controller = new DiscordController(discordAccessService, userRepository);

    user = new User();
    user.setDiscordId("old-id");
    user.setSubscriptionStatus("inactive");
    Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void linkDoesNotGrantActiveSubscriptionWhenDiscordEligible() {
    Mockito.when(discordAccessService.isEligible(Mockito.any(User.class))).thenReturn(true);

    DiscordEligibilityResponse response = controller.link(user, new DiscordLinkRequest("discord-123"));

    Assertions.assertTrue(response.eligible());
    Assertions.assertEquals("inactive", response.status());
    Assertions.assertEquals("inactive", user.getSubscriptionStatus());
    Assertions.assertEquals("discord-123", user.getDiscordId());
    Mockito.verify(userRepository, Mockito.times(1)).save(user);
  }

  @Test
  void checkDoesNotMutateSubscriptionStatusWhenDiscordEligible() {
    user.setDiscordId("discord-123");
    user.setSubscriptionStatus("canceled");
    Mockito.when(discordAccessService.isConfigured()).thenReturn(true);
    Mockito.when(discordAccessService.isEligible(user)).thenReturn(true);

    DiscordEligibilityResponse response = controller.check(user);

    Assertions.assertTrue(response.eligible());
    Assertions.assertEquals("canceled", response.status());
    Assertions.assertEquals("canceled", user.getSubscriptionStatus());
    Mockito.verify(userRepository, Mockito.never()).save(Mockito.any(User.class));
  }
}
