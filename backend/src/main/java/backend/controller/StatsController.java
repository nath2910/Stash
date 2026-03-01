package backend.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import backend.dto.*;
import backend.dto.TopVenteProjection;
import backend.entity.User;
import backend.service.StatsLayoutService;
import backend.service.StatsService;

@RestController
@RequestMapping(path = { "stats", "snkVente/stats" }, produces = APPLICATION_JSON_VALUE)
public class StatsController {

  private final StatsService statsService;
  private final StatsLayoutService statsLayoutService;

  public StatsController(StatsService statsService, StatsLayoutService statsLayoutService) {
    this.statsService = statsService;
    this.statsLayoutService = statsLayoutService;
  }

  private LocalDate pickDate(LocalDate a, LocalDate b) {
    return a != null ? a : b;
  }

  @GetMapping({"/summary"})
  public StatsSummaryResponse summary(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
      @RequestParam(required = false) List<String> categories
  ) {
    LocalDate f = pickDate(start, from);
    LocalDate t = pickDate(end, to);
    return statsService.summary(currentUser.getId(), f, t, asOf, categories);
  }

  @GetMapping({"/timeseries"})
  public List<StatsPointResponse> timeseries(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "day") String granularity,
      @RequestParam(required = false) List<String> categories
  ) {
    LocalDate f = pickDate(start, from);
    LocalDate t = pickDate(end, to);
    return statsService.timeseries(currentUser.getId(), f, t, granularity, categories);
  }

  @GetMapping({"/brands"})
  public List<StatsBreakdownResponse> brands(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) List<String> categories
  ) {
    LocalDate f = pickDate(start, from);
    LocalDate t = pickDate(end, to);
    return statsService.brandBreakdown(currentUser.getId(), f, t, categories);
  }

  @GetMapping({"/top-sales", "/topVentes"})
  public List<TopVenteProjection> topSales(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "3") int limit,
      @RequestParam(required = false) List<String> categories
  ) {
    LocalDate f = pickDate(start, from);
    LocalDate t = pickDate(end, to);
    return statsService.topSales(currentUser.getId(), f, t, limit, categories);
  }

  @GetMapping({"/kpi/{metric}"})
  public StatsKpiResponse kpi(
      @AuthenticationPrincipal User currentUser,
      @PathVariable String metric,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) List<String> categories
  ) {
    LocalDate f = pickDate(start, from);
    LocalDate t = pickDate(end, to);
    return statsService.kpi(currentUser.getId(), f, t, metric, categories);
  }

  @GetMapping({"/series/{metric}"})
  public List<StatsSeriesPointResponse> series(
      @AuthenticationPrincipal User currentUser,
      @PathVariable String metric,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "day") String granularity,
      @RequestParam(required = false) List<String> categories
  ) {
    LocalDate f = pickDate(start, from);
    LocalDate t = pickDate(end, to);
    return statsService.series(currentUser.getId(), f, t, metric, granularity, categories);
  }

  @GetMapping({"/breakdown/{metric}"})
  public List<StatsLabelValueResponse> breakdown(
      @AuthenticationPrincipal User currentUser,
      @PathVariable String metric,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) List<String> categories
  ) {
    LocalDate f = pickDate(start, from);
    LocalDate t = pickDate(end, to);
    return statsService.breakdown(currentUser.getId(), metric, f, t, categories);
  }

  @GetMapping({"/rank/{metric}"})
  public List<StatsLabelValueResponse> rank(
      @AuthenticationPrincipal User currentUser,
      @PathVariable String metric,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(required = false) List<String> categories
  ) {
    LocalDate f = pickDate(start, from);
    LocalDate t = pickDate(end, to);
    return statsService.rank(currentUser.getId(), f, t, metric, limit, categories);
  }

  @GetMapping({"/categories"})
  public List<String> categories(
      @AuthenticationPrincipal User currentUser,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
  ) {
    LocalDate f = from;
    LocalDate t = to;
    return statsService.categories(currentUser.getId(), f, t);
  }

  @GetMapping({"/date-bounds"})
  public StatsDateBoundsResponse dateBounds(@AuthenticationPrincipal User currentUser) {
    return statsService.dateBounds(currentUser.getId());
  }

  @GetMapping({"/layout"})
  public StatsLayoutResponse getLayout(@AuthenticationPrincipal User currentUser) {
    return new StatsLayoutResponse(statsLayoutService.getLayout(currentUser.getId()));
  }

  @PutMapping({"/layout"})
  public StatsLayoutResponse saveLayout(
      @AuthenticationPrincipal User currentUser,
      @RequestBody StatsLayoutRequest request
  ) {
    statsLayoutService.saveLayout(currentUser.getId(), request != null ? request.getLayout() : null);
    return new StatsLayoutResponse(statsLayoutService.getLayout(currentUser.getId()));
  }
}

