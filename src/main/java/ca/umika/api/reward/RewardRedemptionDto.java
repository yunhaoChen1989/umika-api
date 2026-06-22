package ca.umika.api.reward;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record RewardRedemptionDto(
        UUID id,
        UUID userId,
        UUID orderId,
        Integer pointsRedeemed,
        BigDecimal cashValue,
        LocalDateTime createdAt
) {
}
