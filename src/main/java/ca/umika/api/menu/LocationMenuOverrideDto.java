package ca.umika.api.menu;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LocationMenuOverrideDto(
        UUID id,
        UUID locationId,
        String targetType,
        UUID targetId,
        Boolean isVisible,
        Integer sortOrder,
        String customName,
        String customDescription,
        BigDecimal customPrice,
        String customImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
