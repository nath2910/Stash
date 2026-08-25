package backend.service;

import backend.entity.SnkVente;
import backend.entity.User;
import backend.repository.SnkVenteRepository;
import backend.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class SnkVenteServiceDeleteTest {

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
  void deleteBulkDeletesGroupedParentOnlyOnceWhenChildrenAreIncluded() {
    SnkVente parent = new SnkVente();
    parent.setId(10);
    parent.setUser(user);
    parent.setGroupParent(true);

    SnkVente child = new SnkVente();
    child.setId(11);
    child.setUser(user);
    child.setParentId(10);

    Mockito.when(venteRepo.findByUser_IdAndIdIn(1L, List.of(10, 11))).thenReturn(List.of(parent, child));

    int deleted = service.deleteBulk(1L, List.of(10, 11));

    Assertions.assertEquals(2, deleted);
    Mockito.verify(venteRepo).delete(parent);
    Mockito.verify(venteRepo, Mockito.never()).delete(child);
    Mockito.verify(venteRepo, Mockito.never()).countByUser_IdAndParent_Id(Mockito.anyLong(), Mockito.anyInt());
  }

  @Test
  void deleteVenteRemovesEmptyGroupParentAfterLastChildDeletion() {
    SnkVente parent = new SnkVente();
    parent.setId(10);
    parent.setUser(user);
    parent.setGroupParent(true);

    SnkVente child = new SnkVente();
    child.setId(11);
    child.setUser(user);
    child.setParentId(10);

    Mockito.when(venteRepo.findById(11)).thenReturn(Optional.of(child));
    Mockito.when(venteRepo.countByUser_IdAndParent_Id(1L, 10)).thenReturn(0L);
    Mockito.when(venteRepo.findById(10)).thenReturn(Optional.of(parent));

    service.deleteVente(1L, 11);

    Mockito.verify(venteRepo).delete(child);
    Mockito.verify(venteRepo).delete(parent);
  }
}
