package ee.sectorsform.submission;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /** Loads the submission together with its sectors, so it can be mapped outside the transaction. */
    @EntityGraph(attributePaths = "sectors")
    Optional<Submission> findWithSectorsById(Long id);
}
