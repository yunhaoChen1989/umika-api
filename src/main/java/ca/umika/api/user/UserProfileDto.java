package ca.umika.api.user;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record UserProfileDto(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        LocalDate birthday,
        String avatarUrl,
        String preferredLanguage,
        Boolean marketingEmailEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
