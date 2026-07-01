package ca.umika.api.admin;

import java.time.LocalDateTime;
import java.util.UUID;

public record PermissionCodeDto(
        UUID id,
        String permissionGroup,
        String code,
        String name,
        String description,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
