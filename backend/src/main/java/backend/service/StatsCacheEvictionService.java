package backend.service;

import java.util.function.BiConsumer;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Éviction ciblée du cache "statsQueries" : ne vide que les entrées d'un
 * utilisateur donné au lieu de tout le cache (allEntries = true), qui
 * pénalisait tous les utilisateurs à chaque écriture.
 *
 * Les clés de cache sont produites par {@link StatsCacheKeys} au format
 * "<operation>|<userId>|<...suite optionnelle>". On balaie le cache natif
 * Caffeine et on supprime les clés dont le 2e segment (userId) correspond.
 */
@Service
public class StatsCacheEvictionService {

  private static final String CACHE_NAME = "statsQueries";

  private final CacheManager cacheManager;

  public StatsCacheEvictionService(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /**
   * Supprime toutes les entrées de cache appartenant à l'utilisateur donné.
   * Appelé après toute écriture (création / modification / suppression / import).
   */
  public void evictUser(Long userId) {
    Cache cache = cacheManager.getCache(CACHE_NAME);
    if (cache == null) {
      return;
    }
    if (userId == null) {
      cache.clear();
      return;
    }
    com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache =
        (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();

    String prefix = "|" + userId + "|";
    for (Object key : nativeCache.asMap().keySet()) {
      if (key instanceof String s && s.contains(prefix)) {
        cache.evict(key);
      }
    }
  }
}
