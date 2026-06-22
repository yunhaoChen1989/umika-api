package ca.umika.api.reward;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record RewardRuleDto(
        UUID id,
        String ruleKey,
        String ruleValue,
        String description,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
