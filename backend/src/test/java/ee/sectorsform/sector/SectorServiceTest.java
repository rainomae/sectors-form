package ee.sectorsform.sector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Exercises the real {@link SectorService} bean against a mocked repository. A Spring context is
 * needed (rather than a plain Mockito test) so the {@code @Cacheable} proxy engages.
 */
@SpringBootTest
class SectorServiceTest {

    @MockitoBean
    private SectorRepository sectorRepository;

    @Autowired
    private SectorService sectorService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache(SectorService.CACHE).clear();
    }

    @Test
    void findAll_returnsSectorsInSortOrder() {
        var manufacturing = new Sector(1L, "Manufacturing", null, 1);
        var service = new Sector(2L, "Service", null, 2);
        when(sectorRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(manufacturing, service));

        assertThat(sectorService.findAll()).containsExactly(manufacturing, service);
    }

    @Test
    void findAll_readsTheDatabaseOnlyOnce() {
        when(sectorRepository.findAllByOrderBySortOrderAsc())
                .thenReturn(List.of(new Sector(1L, "Manufacturing", null, 1)));

        sectorService.findAll();
        sectorService.findAll();

        verify(sectorRepository, times(1)).findAllByOrderBySortOrderAsc();
    }
}
