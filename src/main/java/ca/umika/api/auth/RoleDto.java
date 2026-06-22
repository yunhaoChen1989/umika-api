package ca.umika.api.auth;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record RoleDto(
        UUID id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
