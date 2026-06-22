package ca.umika.api.reward;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record RewardTransactionDto(
        UUID id,
        UUID userId,
        UUID orderId,
        String type,
        Integer points,
        String source,
        String description,
        LocalDateTime createdAt
) {
}
