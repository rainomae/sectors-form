package ee.sectorsform.submission;

import ee.sectorsform.submission.dto.SubmissionRequest;
import ee.sectorsform.submission.dto.SubmissionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A submission belongs to the HTTP session that created it; only that session can read or update it. */
@RestController
@RequestMapping(value = "/api/submissions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Submissions", description = "Saved forms; each browser session owns at most one")
public class SubmissionController {

    static final String SESSION_ATTRIBUTE = "submissionId";

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Save a new submission and bind it to the current session")
    @ApiResponse(responseCode = "201", description = "Submission saved")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "409", description = "This session already has a submission")
    public SubmissionResponse createSubmission(@RequestBody @Valid SubmissionRequest request, HttpSession session) {
        var existingId = ownedId(session);
        if (existingId != null) {
            throw new SubmissionAlreadyExistsException(existingId);
        }
        var submission = submissionService.create(request.name(), request.sectorIds(), request.agreedToTerms());
        session.setAttribute(SESSION_ATTRIBUTE, submission.getId());
        return SubmissionResponse.from(submission);
    }

    @GetMapping("/current")
    @Operation(summary = "Get the submission saved in the current session")
    @ApiResponse(responseCode = "200", description = "Submission found")
    @ApiResponse(responseCode = "404", description = "Nothing has been saved in this session")
    public SubmissionResponse getCurrentSubmission(HttpSession session) {
        var ownedId = ownedId(session);
        if (ownedId == null) {
            throw SubmissionNotFoundException.noneInSession();
        }
        return SubmissionResponse.from(submissionService.findById(ownedId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a submission owned by the current session")
    @ApiResponse(responseCode = "200", description = "Submission found")
    @ApiResponse(responseCode = "403", description = "Not owned by this session")
    @ApiResponse(responseCode = "404", description = "Submission not found")
    public SubmissionResponse getSubmission(@PathVariable long id, HttpSession session) {
        requireOwnership(id, session);
        return SubmissionResponse.from(submissionService.findById(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a submission owned by the current session")
    @ApiResponse(responseCode = "200", description = "Submission updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "403", description = "Not owned by this session")
    @ApiResponse(responseCode = "404", description = "Submission not found")
    public SubmissionResponse updateSubmission(
            @PathVariable long id, @RequestBody @Valid SubmissionRequest request,
            HttpSession session) {
        requireOwnership(id, session);
        var submission = submissionService.update(id, request.name(), request.sectorIds(), request.agreedToTerms());
        return SubmissionResponse.from(submission);
    }

    private static Long ownedId(HttpSession session) {
        return (Long) session.getAttribute(SESSION_ATTRIBUTE);
    }

    private static void requireOwnership(long id, HttpSession session) {
        if (!Long.valueOf(id).equals(ownedId(session))) {
            throw new SubmissionAccessDeniedException(id);
        }
    }
}
