package ee.sectorsform.sector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the real {@link SectorService} bean against a mocked repository. A Spring context is
 * needed (rather than a plain Mockito test) so the {@code @Cacheable} proxy engages.
 */
@SpringBootTest
class SectorServiceTest {

    private static final Sector MANUFACTURING = new Sector(1L, "Manufacturing", null, 1);
    private static final Sector SERVICE = new Sector(2L, "Service", null, 2);

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
        when(sectorRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(MANUFACTURING, SERVICE));

        assertThat(sectorService.findAll()).containsExactly(MANUFACTURING, SERVICE);
    }

    @Test
    void findAll_readsTheDatabaseOnlyOnce() {
        when(sectorRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(MANUFACTURING));

        sectorService.findAll();
        sectorService.findAll();

        verify(sectorRepository, times(1)).findAllByOrderBySortOrderAsc();
    }

    @Test
    void findAllById_allIdsKnown_returnsTheSectors() {
        when(sectorRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(MANUFACTURING, SERVICE));

        assertThat(sectorService.findAllById(Set.of(1L, 2L))).containsExactlyInAnyOrder(MANUFACTURING, SERVICE);
    }

    @Test
    void findAllById_unknownIds_throwsWithTheUnknownIdsListed() {
        when(sectorRepository.findAllById(Set.of(1L, 999L, 4242L))).thenReturn(List.of(MANUFACTURING));

        assertThatThrownBy(() -> sectorService.findAllById(Set.of(1L, 999L, 4242L)))
                .isInstanceOf(UnknownSectorException.class)
                .hasMessage("Unknown sector ids: [999, 4242]");
    }
}
