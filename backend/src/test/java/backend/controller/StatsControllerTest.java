package backend.controller;

import backend.dto.StatsSummaryResponse;
import backend.entity.User;
import backend.service.StatsLayoutService;
import backend.service.StatsService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class StatsControllerTest {

  @Mock
  private StatsService statsService;

  @Mock
  private StatsLayoutService statsLayoutService;

  private StatsController controller;
  private User user;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    controller = new StatsController(statsService, statsLayoutService);
    user = Mockito.mock(User.class);
    Mockito.when(user.getId()).thenReturn(1L);
  }

  @Test
  void summaryUsesStartEndWhenProvided() {
    LocalDate start = LocalDate.of(2026, 1, 1);
    LocalDate end = LocalDate.of(2026, 1, 31);
    StatsSummaryResponse response = new StatsSummaryResponse(
        BigDecimal.TEN,
        BigDecimal.ONE,
        BigDecimal.ZERO,
        2L,
        3L,
        BigDecimal.valueOf(50)
    );
    Mockito.when(statsService.summary(1L, start, end, null, List.of("Sneakers"), null)).thenReturn(response);

    StatsSummaryResponse result = controller.summary(
        user,
        start,
        end,
        null,
        null,
        null,
        List.of("Sneakers"),
        null
    );

    Assertions.assertSame(response, result);
  }

  @Test
  void summaryFallsBackToFromTo() {
    LocalDate from = LocalDate.of(2026, 2, 1);
    LocalDate to = LocalDate.of(2026, 2, 28);
    StatsSummaryResponse response = new StatsSummaryResponse(
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0L,
        0L,
        BigDecimal.ZERO
    );
    Mockito.when(statsService.summary(1L, from, to, null, null, null)).thenReturn(response);

    StatsSummaryResponse result = controller.summary(
        user,
        null,
        null,
        from,
        to,
        null,
        null,
        null
    );

    Assertions.assertSame(response, result);
  }
}
