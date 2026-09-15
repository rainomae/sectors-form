package ee.sectorsform.submission.dto;

import ee.sectorsform.sector.Sector;
import ee.sectorsform.submission.Submission;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record SubmissionResponse(
        Long id,
        String name,
        List<Long> sectorIds,
        boolean agreedToTerms,
        Instant createdAt,
        Instant updatedAt) {

    public static SubmissionResponse from(Submission submission) {
        List<Long> sectorIds = submission.getSectors().stream()
                .sorted(Comparator.comparingInt(Sector::getSortOrder))
                .map(Sector::getId)
                .toList();
        return new SubmissionResponse(
                submission.getId(),
                submission.getName(),
                sectorIds,
                submission.isAgreedToTerms(),
                submission.getCreatedAt(),
                submission.getUpdatedAt());
    }
}
