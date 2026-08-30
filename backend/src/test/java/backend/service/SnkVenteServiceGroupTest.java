package backend.service;

import backend.dto.SnkVenteGroupViewDto;
import backend.entity.SnkVente;
import backend.entity.User;
import backend.repository.SnkVenteRepository;
import backend.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SnkVenteServiceGroupTest {

  private SnkVenteRepository venteRepo;
  private UserRepository userRepo;
  private StatsCacheEvictionService cacheEviction;
  private snkVenteService service;
  private User user;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    venteRepo = Mockito.mock(SnkVenteRepository.class);
    userRepo = Mockito.mock(UserRepository.class);
    cacheEviction = Mockito.mock(StatsCacheEvictionService.class);
    Mockito.doNothing().when(cacheEviction).evictUser(Mockito.anyLong());
    service = new snkVenteService(venteRepo, userRepo, cacheEviction);

    user = Mockito.mock(User.class);
    Mockito.when(user.getId()).thenReturn(1L);
    Mockito.when(userRepo.findById(1L)).thenReturn(Optional.of(user));
    Mockito.when(venteRepo.save(Mockito.any(SnkVente.class))).thenAnswer(invocation -> {
      SnkVente vente = invocation.getArgument(0);
      if (vente.getId() == null) vente.setId(50);
      return vente;
    });
    Mockito.when(venteRepo.saveAll(Mockito.anyList())).thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void regrouperSelectionCreeUnParentEtAttacheLesLignes() {
    SnkVente first = standalone(11, "Jordan 1 Chicago 42", "Jordan", "SNEAKER");
    first.setDateAchat(LocalDate.of(2026, 8, 10));
    first.setPrixRetail(BigDecimal.valueOf(180));

    SnkVente second = standalone(12, "Jordan 1 Chicago 43", "Jordan", "SNEAKER");
    second.setDateAchat(LocalDate.of(2026, 8, 12));
    second.setPrixRetail(BigDecimal.valueOf(180));
    second.setPrixResell(BigDecimal.valueOf(255));
    second.setDateVente(LocalDate.of(2026, 8, 20));

    Mockito.when(venteRepo.findByUser_IdAndIdIn(1L, List.of(11, 12))).thenReturn(List.of(first, second));

    SnkVenteGroupViewDto grouped = service.regrouperSelection(1L, List.of(11, 12));

    Assertions.assertEquals(50, grouped.id());
    Assertions.assertTrue(grouped.groupParent());
    Assertions.assertEquals(2, grouped.quantity());
    Assertions.assertEquals(1, grouped.soldCount());
    Assertions.assertEquals("Lot 2 - Jordan", grouped.nomItem());
    Assertions.assertEquals("Jordan", grouped.categorie());
    Assertions.assertEquals("SNEAKER", grouped.type());
    Assertions.assertEquals(LocalDate.of(2026, 8, 10), grouped.dateAchat());
    Assertions.assertEquals(2, grouped.children().size());
    Assertions.assertEquals(1, grouped.children().get(0).unitIndex());
    Assertions.assertEquals(2, grouped.children().get(1).unitIndex());

    Mockito.verify(venteRepo).save(Mockito.argThat(parentMatcher()));
    Mockito.verify(venteRepo).saveAll(Mockito.argThat(childrenMatcher(50)));
    Mockito.verify(cacheEviction).evictUser(1L);
  }

  @Test
  void regrouperSelectionRefuseLesLignesDejaDansUnGroupe() {
    SnkVente child = standalone(11, "Jordan 1", "Jordan", "SNEAKER");
    child.setParentId(77);
    SnkVente plain = standalone(12, "Jordan 1", "Jordan", "SNEAKER");

    Mockito.when(venteRepo.findByUser_IdAndIdIn(1L, List.of(11, 12))).thenReturn(List.of(child, plain));

    ResponseStatusException error = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.regrouperSelection(1L, List.of(11, 12))
    );

    Assertions.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    Mockito.verify(venteRepo, Mockito.never()).save(Mockito.any(SnkVente.class));
    Mockito.verify(venteRepo, Mockito.never()).saveAll(Mockito.anyList());
  }

  @Test
  void regrouperSelectionRefuseLesSelectionsIncompatibles() {
    SnkVente first = standalone(11, "Jordan 1", "Jordan", "SNEAKER");
    SnkVente second = standalone(12, "One Piece", "Booster", "COLLECTIBLE");

    Mockito.when(venteRepo.findByUser_IdAndIdIn(1L, List.of(11, 12))).thenReturn(List.of(first, second));

    ResponseStatusException error = Assertions.assertThrows(
        ResponseStatusException.class,
        () -> service.regrouperSelection(1L, List.of(11, 12))
    );

    Assertions.assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    Assertions.assertTrue(String.valueOf(error.getReason()).contains("meme type"));
  }

  @Test
  void updateVenteDuParentRepartitLesMontantsEtDatesSurChaqueSousItem() {
    SnkVente parent = standalone(50, "Lot tickets", null, "TICKET");
    parent.setGroupParent(true);

    SnkVente first = standalone(11, "ticket A", null, "TICKET");
    first.setParentId(50);
    SnkVente second = standalone(12, "ticket B", null, "TICKET");
    second.setParentId(50);
    SnkVente third = standalone(13, "ticket C", null, "TICKET");
    third.setParentId(50);

    Mockito.when(venteRepo.findById(50)).thenReturn(Optional.of(parent));
    Mockito.when(venteRepo.findByParent_IdOrderByUnitIndexAscIdAsc(50)).thenReturn(List.of(first, second, third));

    SnkVente payload = new SnkVente();
    payload.setNomItem("Lot final Roland Garros");
    payload.setType("TICKET");
    payload.setCategorie("Tickets");
    payload.setPrixRetail(new BigDecimal("100.00"));
    payload.setPrixResell(new BigDecimal("200.00"));
    payload.setDateAchat(LocalDate.of(2026, 2, 9));
    payload.setDateVente(LocalDate.of(2026, 2, 18));
    payload.setDescription("Vente groupee");
    payload.setMetadata(Map.of("venue", "Roland Garros"));

    SnkVente saved = service.updateVente(1L, 50, payload);

    Assertions.assertEquals(50, saved.getId());
    Assertions.assertEquals(new BigDecimal("100.00"), parent.getPrixRetail());
    Assertions.assertEquals(new BigDecimal("200.00"), parent.getPrixResell());
    Assertions.assertEquals(LocalDate.of(2026, 2, 18), parent.getDateVente());

    Assertions.assertEquals(new BigDecimal("33.34"), first.getPrixRetail());
    Assertions.assertEquals(new BigDecimal("33.33"), second.getPrixRetail());
    Assertions.assertEquals(new BigDecimal("33.33"), third.getPrixRetail());
    Assertions.assertEquals(new BigDecimal("66.67"), first.getPrixResell());
    Assertions.assertEquals(new BigDecimal("66.67"), second.getPrixResell());
    Assertions.assertEquals(new BigDecimal("66.66"), third.getPrixResell());
    Assertions.assertEquals(LocalDate.of(2026, 2, 18), first.getDateVente());
    Assertions.assertEquals(LocalDate.of(2026, 2, 18), second.getDateVente());
    Assertions.assertEquals(LocalDate.of(2026, 2, 18), third.getDateVente());
    Assertions.assertNull(first.getCategorie());
    Assertions.assertNull(second.getCategorie());
    Assertions.assertNull(third.getCategorie());

    Mockito.verify(venteRepo).saveAll(Mockito.argThat(rows -> {
      if (rows == null) return false;
      List<SnkVente> savedRows = new ArrayList<>();
      rows.forEach(savedRows::add);
      return savedRows.size() == 3
          && savedRows.stream().allMatch(child ->
              child.getParentId() == 50 || (child.getParent() != null && Integer.valueOf(50).equals(child.getParent().getId()))
          );
    }));
  }

  private SnkVente standalone(int id, String name, String category, String type) {
    SnkVente vente = new SnkVente();
    vente.setId(id);
    vente.setUser(user);
    vente.setNomItem(name);
    vente.setCategorie(category);
    vente.setType(type);
    vente.setGroupParent(false);
    return vente;
  }

  private ArgumentMatcher<SnkVente> parentMatcher() {
    return parent ->
        parent != null
            && parent.isGroupParent()
            && parent.getParent() == null
            && "Lot 2 - Jordan".equals(parent.getNomItem())
            && "Jordan".equals(parent.getCategorie())
            && "SNEAKER".equals(parent.getType());
  }

  private ArgumentMatcher<Iterable<SnkVente>> childrenMatcher(int parentId) {
    return rows ->
        rows != null
            && iterableMatchesParent(rows, parentId);
  }

  private boolean iterableMatchesParent(Iterable<SnkVente> rows, int parentId) {
    List<SnkVente> savedRows = new ArrayList<>();
    rows.forEach(savedRows::add);
    return savedRows.size() == 2
        && savedRows.stream().allMatch(child ->
            child.getParent() != null
                && Integer.valueOf(parentId).equals(child.getParent().getId())
                && child.getUnitIndex() != null
                && child.getUnitIndex() >= 1
        );
  }
}
