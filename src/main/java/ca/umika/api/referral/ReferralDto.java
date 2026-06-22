package ca.umika.api.referral;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record ReferralDto(
        UUID id,
        UUID referrerId,
        UUID referredUserId,
        String status,
        String referralCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
