package backend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
   
    Optional<User> findByProviderAndProviderId(String provider, String providerId);

    Optional<User> findByStripeCustomerId(String stripeCustomerId);

    @Query("select u.id from User u")
    List<Long> findAllUserIds();
    
}
