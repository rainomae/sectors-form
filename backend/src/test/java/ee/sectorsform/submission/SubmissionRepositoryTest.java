package ee.sectorsform.submission;

import ee.sectorsform.sector.Sector;
import ee.sectorsform.sector.SectorRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class SubmissionRepositoryTest {

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void save_persistsTheSectorsAndSetsTheTimestamps() {
        var saved = submissionRepository.saveAndFlush(new Submission("Mari", sectors(6L, 42L), true));
        entityManager.clear();

        var reloaded = submissionRepository.findWithSectorsById(saved.getId()).orElseThrow();

        assertThat(reloaded.getName()).isEqualTo("Mari");
        assertThat(reloaded.isAgreedToTerms()).isTrue();
        assertThat(reloaded.getSectors()).extracting(Sector::getId).containsExactlyInAnyOrder(6L, 42L);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void update_replacesTheSectors() {
        var saved = submissionRepository.saveAndFlush(new Submission("Mari", sectors(6L), true));

        saved.update("Mari Tamm", sectors(42L, 113L), true);
        submissionRepository.saveAndFlush(saved);
        entityManager.clear();

        var reloaded = submissionRepository.findWithSectorsById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("Mari Tamm");
        assertThat(reloaded.getSectors()).extracting(Sector::getId).containsExactlyInAnyOrder(42L, 113L);
    }

    @Test
    void findWithSectorsById_unknownId_returnsEmpty() {
        assertThat(submissionRepository.findWithSectorsById(987654321L)).isEmpty();
    }

    @Test
    void save_declinedTerms_isRejectedByTheDatabaseConstraint() {
        var declined = new Submission("Mari", sectors(6L), false);

        assertThatThrownBy(() -> submissionRepository.saveAndFlush(declined))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Set<Sector> sectors(Long... ids) {
        return new HashSet<>(sectorRepository.findAllById(Set.of(ids)));
    }
}
