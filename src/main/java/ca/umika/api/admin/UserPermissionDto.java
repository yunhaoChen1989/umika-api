package ca.umika.api.admin;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record UserPermissionDto(
        UUID id,
        UUID userId,
        String permissionCode,
        Boolean isGranted,
        LocalDateTime createdAt
) {
}
