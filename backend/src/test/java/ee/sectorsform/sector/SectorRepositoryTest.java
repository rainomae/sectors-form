package ee.sectorsform.sector;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** Guards the seeded sector data: every option of the original select box, with its hierarchy and order. */
@DataJpaTest
class SectorRepositoryTest {

    @Autowired
    private SectorRepository sectorRepository;

    @Test
    void findAllByOrderBySortOrderAsc_returnsAllSeededSectorsInDisplayOrder() {
        List<Sector> sectors = sectorRepository.findAllByOrderBySortOrderAsc();

        assertThat(sectors).hasSize(79);
        assertThat(sectors).extracting(Sector::getSortOrder).isSorted().doesNotHaveDuplicates();
        assertThat(sectors)
                .extracting(Sector::getId)
                .startsWith(1L, 19L, 18L, 6L, 342L)
                .endsWith(112L, 113L);
        assertThat(sectors)
                .filteredOn(sector -> sector.getParentId() == null)
                .extracting(Sector::getName)
                .containsExactly("Manufacturing", "Other", "Service");
    }

    @Test
    void seed_derivesFourLevelsFromTheIndentation() {
        Map<Long, Sector> byId = sectorRepository.findAll().stream()
                .collect(Collectors.toMap(Sector::getId, Function.identity()));

        Map<Integer, Long> countByDepth = byId.values().stream()
                .collect(Collectors.groupingBy(s -> depthOf(s, byId), Collectors.counting()));
        assertThat(countByDepth).containsExactlyInAnyOrderEntriesOf(Map.of(0, 3L, 1, 19L, 2, 47L, 3, 10L));

        // Aluminium and steel workboats -> Maritime -> Machinery -> Manufacturing
        assertThat(byId.get(271L).getParentId()).isEqualTo(97L);
        assertThat(byId.get(97L).getParentId()).isEqualTo(12L);
        assertThat(byId.get(12L).getParentId()).isEqualTo(1L);
        // Water -> Transport and Logistics -> Service
        assertThat(byId.get(113L).getParentId()).isEqualTo(21L);
        assertThat(byId.get(21L).getParentId()).isEqualTo(2L);
    }

    @Test
    void seed_trimsAndDecodesLabels() {
        assertThat(sectorRepository.findById(42L)).map(Sector::getName).hasValue("Fish & fish products");
        assertThat(sectorRepository.findById(390L)).map(Sector::getName).hasValue("Children’s room");
        assertThat(sectorRepository.findById(5L)).map(Sector::getName).hasValue("Printing");
        assertThat(sectorRepository.findAll()).extracting(Sector::getName).allSatisfy(
                name -> assertThat(name)
                        .isEqualTo(name.strip()));
    }

    private static int depthOf(Sector sector, Map<Long, Sector> byId) {
        int depth = 0;
        for (Long parentId = sector.getParentId(); parentId != null; parentId = byId.get(parentId).getParentId()) {
            depth++;
        }
        return depth;
    }
}
