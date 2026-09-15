package ee.sectorsform.submission.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record SubmissionRequest(
        @NotBlank(message = "Name is required") @Size(max = 255, message = "Name must be at most 255 characters") String name,

        @NotEmpty(message = "Select at least one sector") Set<@NotNull(message = "Sector id must not be null") Long> sectorIds,

        @NotNull(message = "You must agree to the terms") @AssertTrue(message = "You must agree to the terms") Boolean agreedToTerms) {
}
