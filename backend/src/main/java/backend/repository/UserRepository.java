package backend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("update User u set u.sessionVersion = u.sessionVersion + 1 where u.id = :id")
    void revokeSessions(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.stripeCustomerId = :customer")
    Optional<User> lockByStripeCustomerId(@Param("customer") String customer);

    Optional<User> findByEmail(String email);
   
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    @Query("select u.id from User u")
    List<Long> findAllUserIds();
    
}
