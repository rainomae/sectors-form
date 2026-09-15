package ee.sectorsform.shared;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404WithMessage() {
        var response = handler.handleNotFound(new NotFoundException("Thing not found: 1") {
        });

        assertProblem(response, HttpStatus.NOT_FOUND, "Thing not found: 1");
    }

    @Test
    void handleForbidden_returns403WithMessage() {
        var response = handler.handleForbidden(new ForbiddenException("Not yours") {
        });

        assertProblem(response, HttpStatus.FORBIDDEN, "Not yours");
    }

    @Test
    void handleConflict_returns409WithMessage() {
        var response = handler.handleConflict(new ConflictException("Already there") {
        });

        assertProblem(response, HttpStatus.CONFLICT, "Already there");
    }

    @Test
    void handleBadRequest_returns400WithMessage() {
        var response = handler.handleBadRequest(new BadRequestException("Unknown sector ids: [999]") {
        });

        assertProblem(response, HttpStatus.BAD_REQUEST, "Unknown sector ids: [999]");
    }

    @Test
    void handleUnexpected_returnsGeneric500WithoutInternalDetails() {
        var response = handler.handleUnexpected(new IllegalStateException("connection pool exhausted"));

        assertProblem(response, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private static void assertProblem(ResponseEntity<ProblemDetail> response, HttpStatus status, String detail) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getStatus()).isEqualTo(status.value());
        assertThat(body.getDetail()).isEqualTo(detail);
    }
}
