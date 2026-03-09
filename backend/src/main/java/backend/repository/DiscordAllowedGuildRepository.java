package backend.repository;

import backend.entity.DiscordAllowedGuild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiscordAllowedGuildRepository extends JpaRepository<DiscordAllowedGuild, Long> {
  Optional<DiscordAllowedGuild> findByGuildId(String guildId);
}
