package ee.sectorsform.submission;

import ee.sectorsform.shared.NotFoundException;

public class SubmissionNotFoundException extends NotFoundException {

    public SubmissionNotFoundException(long id) {
        super("Submission not found: " + id);
    }

    private SubmissionNotFoundException(String message) {
        super(message);
    }

    static SubmissionNotFoundException noneInSession() {
        return new SubmissionNotFoundException("No submission has been saved in this session");
    }
}
