package ca.umika.api.reward;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record RewardWalletDto(
        UUID id,
        UUID userId,
        Integer totalEarned,
        Integer totalRedeemed,
        Integer availableBalance,
        LocalDateTime updatedAt
) {
}
