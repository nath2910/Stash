package backend.controller;

import backend.dto.StatsSummaryResponse;
import backend.entity.User;
import backend.service.StatsLayoutService;
import backend.service.StatsService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatsControllerTest {

  private MockMvc mvc;

  @Mock
  StatsService statsService;

  @Mock
  StatsLayoutService statsLayoutService;

  private User mockUser;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    StatsController controller = new StatsController(statsService, statsLayoutService);
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
    mockUser = Mockito.mock(User.class);
    Mockito.when(mockUser.getId()).thenReturn(1L);
  }

  private UsernamePasswordAuthenticationToken authPrincipal() {
    return new UsernamePasswordAuthenticationToken(mockUser, null);
  }

  @Test
  void summaryEndpointReturns200() throws Exception {
    Mockito.when(statsService.summary(Mockito.eq(1L), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(new StatsSummaryResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO));

    mvc.perform(get("/stats/summary")
            .accept(MediaType.APPLICATION_JSON)
            .principal(authPrincipal()))
        .andExpect(status().isOk());
  }

  @Test
  void layoutEndpointReturns200() throws Exception {
    Mockito.when(statsLayoutService.getLayout(1L)).thenReturn(null);

    mvc.perform(get("/stats/layout")
            .accept(MediaType.APPLICATION_JSON)
            .principal(authPrincipal()))
        .andExpect(status().isOk());
  }
}
