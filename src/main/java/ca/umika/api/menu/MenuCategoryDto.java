package ca.umika.api.menu;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record MenuCategoryDto(
        UUID id,
        UUID locationId,
        String name,
        String description,
        Integer sortOrder,
        Boolean isActive,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
