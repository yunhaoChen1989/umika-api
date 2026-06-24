package ca.umika.api.user;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserWriteRequest(
        String email,
        String phone,
        String password,
        UUID roleId,
        Boolean emailVerified,
        LocalDateTime emailVerifiedAt,
        String referralCode,
        UUID referredBy,
        String stripeCustomerId,
        Boolean isActive,
        UUID location,
        LocalDateTime lastLoginAt,
        String lastLoginIp
) {
}
