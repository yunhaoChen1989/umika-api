package ca.umika.api.user;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String phone,
        String passwordHash,
        Boolean emailVerified,
        LocalDateTime emailVerifiedAt,
        String referralCode,
        UUID referredBy,
        String stripeCustomerId,
        Boolean isActive,
        LocalDateTime lastLoginAt,
        String lastLoginIp,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
