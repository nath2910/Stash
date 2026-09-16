package backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import backend.entity.SnkVente;
import backend.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

/**
 * Database-level regression test for tenant isolation. It deliberately uses
 * two persisted users: no mocked ownership predicate can make this pass.
 */
@DataJpaTest(properties = {
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:isolation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SnkVenteRepositoryIsolationIntegrationTest {

  @Autowired
  private SnkVenteRepository repository;

  @Autowired
  private UserRepository userRepository;

  @Test
  void oneUserCannotReadOrDeleteAnotherUsersInventoryRow() {
    User userA = createUser("user-a@example.test");
    User userB = createUser("user-b@example.test");
    SnkVente itemA = createItem(userA, "A-only");
    SnkVente itemB = createItem(userB, "B-only");

    repository.flush();

    assertThat(repository.findByIdAndUser_Id(itemB.getId(), userA.getId())).isEmpty();
    assertThat(repository.findByUser_IdOrderByDateAchatDesc(userA.getId()))
        .extracting(SnkVente::getNomItem)
        .containsExactly("A-only");

    repository.deleteByIdAndUser_Id(itemB.getId(), userA.getId());
    repository.flush();

    assertThat(repository.findByIdAndUser_Id(itemA.getId(), userA.getId())).isPresent();
    assertThat(repository.findByIdAndUser_Id(itemB.getId(), userB.getId())).isPresent();
  }

  private User createUser(String email) {
    User user = new User();
    user.setEmail(email);
    user.setFirstName("Test");
    user.setLastName("User");
    user.setPassword("not-used-by-this-test");
    user.setEmailVerified(true);
    return userRepository.saveAndFlush(user);
  }

  private SnkVente createItem(User user, String name) {
    SnkVente item = new SnkVente();
    item.setUser(user);
    item.setNomItem(name);
    item.setDateAchat(LocalDate.of(2026, 1, 1));
    item.setPrixRetail(BigDecimal.TEN);
    item.setType("SNEAKER");
    item.setGroupParent(false);
    return repository.save(item);
  }
}
