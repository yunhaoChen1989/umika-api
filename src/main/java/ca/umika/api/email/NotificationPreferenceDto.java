package ca.umika.api.email;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record NotificationPreferenceDto(
        UUID id,
        UUID userId,
        Boolean emailEnabled,
        Boolean smsEnabled,
        Boolean marketingEnabled,
        Boolean orderUpdatesEnabled,
        Boolean referralUpdatesEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
