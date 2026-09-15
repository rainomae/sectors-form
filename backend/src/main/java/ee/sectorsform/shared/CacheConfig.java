package ee.sectorsform.shared;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enables Spring's in-memory cache. The only cached data is the sector list, which changes only
 * through a Flyway migration at startup, so nothing ever needs to be evicted.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
