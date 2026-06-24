package ca.umika.api.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String phone,
        Boolean emailVerified,
        LocalDateTime emailVerifiedAt,
        String referralCode,
        UUID referredBy,
        String stripeCustomerId,
        Boolean isActive,
        UUID location,
        LocalDateTime lastLoginAt,
        String lastLoginIp,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
