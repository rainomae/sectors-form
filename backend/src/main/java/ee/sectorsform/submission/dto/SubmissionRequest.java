package ee.sectorsform.submission.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.regex.Pattern;

public record SubmissionRequest(
        @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must be at most 255 characters") String name,

        @NotEmpty(message = "Select at least one sector") Set<@NotNull(message = "Sector id must not be null") Long> sectorIds,

        @NotNull(message = "You must agree to the terms") @AssertTrue(message = "You must agree to the terms") Boolean agreedToTerms) {

    // The characters JavaScript's String.prototype.trim removes (ASCII whitespace, Unicode separators including
    // the no-break space, and the byte order mark), so the browser and the server agree on what is blank.
    private static final Pattern SURROUNDING_WHITESPACE = Pattern.compile("^[\\s\\p{Z}\\uFEFF]+|[\\s\\p{Z}\\uFEFF]+$");

    /** Trims the name before validation, so the blank check and the length limit apply to what is stored. */
    public SubmissionRequest {
        name = name == null ? null : SURROUNDING_WHITESPACE.matcher(name).replaceAll("");
    }
}
