package ca.umika.api.admin;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record RolePermissionDto(
        UUID id,
        UUID roleId,
        String permissionCode,
        String description,
        LocalDateTime createdAt
) {
}
