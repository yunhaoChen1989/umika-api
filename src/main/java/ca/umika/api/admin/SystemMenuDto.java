package ca.umika.api.admin;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record SystemMenuDto(
        UUID id,
        UUID parentId,
        String name,
        String description,
        String nameEn,
        String nameZh,
        String nameKo,
        String descriptionEn,
        String descriptionZh,
        String descriptionKo,
        String code,
        String path,
        String component,
        String icon,
        String menuType,
        Integer sortOrder,
        Boolean isVisible,
        Boolean isEnabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
