package ee.sectorsform.submission;

import ee.sectorsform.shared.ForbiddenException;

/** The submission was not created by the current session. Thrown whether or not the id exists, so ids cannot be probed. */
public class SubmissionAccessDeniedException extends ForbiddenException {

    public SubmissionAccessDeniedException(long id) {
        super("Submission " + id + " does not belong to the current session");
    }
}
