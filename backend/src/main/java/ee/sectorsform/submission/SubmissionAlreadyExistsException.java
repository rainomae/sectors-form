package ee.sectorsform.submission;

import ee.sectorsform.shared.ConflictException;

/** A session owns at most one submission; a second save must update the existing one. */
public class SubmissionAlreadyExistsException extends ConflictException {

    public SubmissionAlreadyExistsException(long existingId) {
        super("This session already has submission " + existingId + "; update it instead");
    }
}
