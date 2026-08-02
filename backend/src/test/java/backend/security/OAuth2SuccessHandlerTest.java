package backend.security;

import backend.entity.User;
import backend.repository.UserRepository;
import backend.service.DiscordAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

class OAuth2SuccessHandlerTest {

  private UserRepository userRepository;
  private JwtService jwtService;
  private DiscordAccessService discordAccessService;
  private OAuth2SuccessHandler handler;

  @BeforeEach
  void setUp() {
    userRepository = Mockito.mock(UserRepository.class);
    jwtService = Mockito.mock(JwtService.class);
    discordAccessService = Mockito.mock(DiscordAccessService.class);
    handler = new OAuth2SuccessHandler(userRepository, jwtService, discordAccessService, new ObjectMapper());
    ReflectionTestUtils.setField(handler, "successRedirect", "https://mystash.test/auth/callback");
    Mockito.when(jwtService.generateToken(Mockito.anyLong())).thenReturn("jwt-token");
    Mockito.when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void discordOauthDoesNotPromoteSubscriptionStatus() throws Exception {
    OAuth2User oauthUser = Mockito.mock(OAuth2User.class);
    Authentication authentication = Mockito.mock(Authentication.class);
    HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

    User existingUser = new User();
    existingUser.setEmail("user@example.com");
    existingUser.setProvider("DISCORD");
    existingUser.setProviderId("discord-123");
    existingUser.setDiscordId("discord-123");
    existingUser.setFirstName("Discord User");
    existingUser.setPassword("");
    existingUser.setSubscriptionStatus("inactive");
    ReflectionTestUtils.setField(existingUser, "id", 7L);

    Mockito.when(authentication.getPrincipal()).thenReturn(oauthUser);
    Mockito.when(oauthUser.getAttributes()).thenReturn(Map.of(
        "id", "discord-123",
        "email", "user@example.com",
        "verified", Boolean.TRUE,
        "username", "Discord User"
    ));
    Mockito.when(discordAccessService.isEligible(Mockito.any(User.class))).thenReturn(true);
    Mockito.when(userRepository.findByProviderAndProviderId("DISCORD", "discord-123"))
        .thenReturn(Optional.empty());
    Mockito.when(userRepository.findByEmail("user@example.com"))
        .thenReturn(Optional.of(existingUser));

    handler.onAuthenticationSuccess(null, response, authentication);

    Assertions.assertEquals("inactive", existingUser.getSubscriptionStatus());
    Mockito.verify(userRepository, Mockito.never()).save(Mockito.argThat(user ->
        "active".equals(((User) user).getSubscriptionStatus())
    ));
    Mockito.verify(response).sendRedirect(Mockito.contains("#token="));
  }
}
