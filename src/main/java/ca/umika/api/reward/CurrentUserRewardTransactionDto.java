package ca.umika.api.reward;

import java.time.LocalDateTime;
import java.util.UUID;

public record CurrentUserRewardTransactionDto(
        UUID id,
        String type,
        Integer points,
        String description,
        UUID orderId,
        String orderNumber,
        LocalDateTime createdAt
) {
}
