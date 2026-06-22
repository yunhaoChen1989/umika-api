package ca.umika.api.auth;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record RefreshTokenDto(
        UUID id,
        UUID userId,
        String tokenHash,
        LocalDateTime expiresAt,
        Boolean revoked,
        LocalDateTime createdAt
) {
}
