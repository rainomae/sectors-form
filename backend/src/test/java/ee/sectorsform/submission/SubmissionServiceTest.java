package ee.sectorsform.submission;

import ee.sectorsform.sector.SectorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static ee.sectorsform.submission.SubmissionRequests.FISH;
import static ee.sectorsform.submission.SubmissionRequests.FOOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private SectorService sectorService;

    @InjectMocks
    private SubmissionService submissionService;

    @Test
    void create_resolvesTheSectorsAndSaves() {
        when(sectorService.findAllById(Set.of(6L, 42L))).thenReturn(Set.of(FOOD, FISH));
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var saved = submissionService.create("Mari Maasikas", Set.of(6L, 42L), true);

        assertThat(saved.getName()).isEqualTo("Mari Maasikas");
        assertThat(saved.getSectors()).containsExactlyInAnyOrder(FOOD, FISH);
        assertThat(saved.isAgreedToTerms()).isTrue();
        verify(submissionRepository).save(saved);
    }

    @Test
    void update_appliesEveryChangeToTheExistingSubmission() {
        var existing = new Submission("Mari", Set.of(FOOD), true);
        when(submissionRepository.findWithSectorsById(1L)).thenReturn(Optional.of(existing));
        when(sectorService.findAllById(Set.of(42L))).thenReturn(Set.of(FISH));

        var updated = submissionService.update(1L, "Mari Tamm", Set.of(42L), true);

        assertThat(updated).isSameAs(existing);
        assertThat(updated.getName()).isEqualTo("Mari Tamm");
        assertThat(updated.getSectors()).containsExactly(FISH);
        verify(submissionRepository).flush();
    }

    @Test
    void update_unknownId_throwsNotFound() {
        when(submissionRepository.findWithSectorsById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.update(5L, "Mari", Set.of(1L), true))
                .isInstanceOf(SubmissionNotFoundException.class)
                .hasMessage("Submission not found: 5");
        verifyNoInteractions(sectorService);
    }

    @Test
    void findById_returnsTheSubmissionWithItsSectors() {
        var existing = new Submission("Mari", Set.of(FOOD, FISH), true);
        when(submissionRepository.findWithSectorsById(1L)).thenReturn(Optional.of(existing));

        assertThat(submissionService.findById(1L)).isSameAs(existing);
    }

    @Test
    void findById_unknownId_throwsNotFound() {
        when(submissionRepository.findWithSectorsById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> submissionService.findById(5L))
                .isInstanceOf(SubmissionNotFoundException.class)
                .hasMessage("Submission not found: 5");
    }
}
