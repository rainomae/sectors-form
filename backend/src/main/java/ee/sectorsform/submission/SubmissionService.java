package ee.sectorsform.submission;

import ee.sectorsform.sector.SectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final SubmissionRepository submissionRepository;
    private final SectorService sectorService;

    public SubmissionService(SubmissionRepository submissionRepository, SectorService sectorService) {
        this.submissionRepository = submissionRepository;
        this.sectorService = sectorService;
    }

    @Transactional
    public Submission create(String name, Set<Long> sectorIds, boolean agreedToTerms) {
        var submission = new Submission(name, sectorService.findAllById(sectorIds), agreedToTerms);
        var saved = submissionRepository.save(submission);
        log.info("Submission created: id={}, sectors={}", saved.getId(), sectorIds.size());
        return saved;
    }

    @Transactional
    public Submission update(long id, String name, Set<Long> sectorIds, boolean agreedToTerms) {
        var submission = submissionRepository.findWithSectorsById(id)
                .orElseThrow(() -> new SubmissionNotFoundException(id));
        submission.update(name, sectorService.findAllById(sectorIds), agreedToTerms);
        submissionRepository.flush();
        log.info("Submission updated: id={}, sectors={}", id, sectorIds.size());
        return submission;
    }

    @Transactional(readOnly = true)
    public boolean exists(long id) {
        return submissionRepository.existsById(id);
    }

    @Transactional(readOnly = true)
    public Submission findById(long id) {
        return submissionRepository.findWithSectorsById(id).orElseThrow(() -> new SubmissionNotFoundException(id));
    }
}
