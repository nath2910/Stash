package backend.service;

import backend.dto.SnkVenteImportDto;
import backend.entity.SnkVente;
import backend.entity.User;
import backend.repository.SnkVenteRepository;
import backend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

class SnkVenteServiceImportTest {

  private SnkVenteRepository venteRepo;
  private UserRepository userRepo;
  private snkVenteService service;
  private User user;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    venteRepo = Mockito.mock(SnkVenteRepository.class);
    userRepo = Mockito.mock(UserRepository.class);
    service = new snkVenteService(venteRepo, userRepo);

    user = Mockito.mock(User.class);
    Mockito.when(user.getId()).thenReturn(1L);
    Mockito.when(userRepo.findById(1L)).thenReturn(Optional.of(user));
  }

  @Test
  void refuseTooManyRows() {
    List<SnkVenteImportDto> big = java.util.Collections.nCopies(
        600,
        new SnkVenteImportDto("A", null, null, null, null, null, null, null, null)
    );
    Assertions.assertThrows(ResponseStatusException.class, () -> service.importBulk(1L, big));
  }

  @Test
  void refusesEmptyPayload() {
    Assertions.assertThrows(ResponseStatusException.class, () -> service.importBulk(1L, List.of()));
  }

  @Test
  void importsTrimmedNames() {
    SnkVenteImportDto dto = new SnkVenteImportDto(
        "  Test  ",
        null,
        null,
        null,
        null,
        null,
        " cat ",
        null,
        null
    );
    int created = service.importBulk(1L, List.of(dto));
    Assertions.assertEquals(1, created);

    ArgumentCaptor<List<SnkVente>> captor = ArgumentCaptor.forClass(List.class);
    Mockito.verify(venteRepo).saveAll(captor.capture());
    List<SnkVente> saved = captor.getValue();
    Assertions.assertEquals(1, saved.size());
    Assertions.assertEquals("Test", saved.get(0).getNomItem());
  }
}
