package ca.umika.api.reward;

import org.springframework.stereotype.Component;

@Component
public class RewardRuleMapper {

    public RewardRuleDto toDto(RewardRuleEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RewardRuleDto(
                entity.getId(),
                entity.getRuleKey(),
                entity.getRuleValue(),
                entity.getDescription(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public RewardRuleEntity toEntity(RewardRuleDto dto) {
        RewardRuleEntity entity = new RewardRuleEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(RewardRuleEntity entity, RewardRuleDto dto) {
        entity.setRuleKey(dto.ruleKey());
        entity.setRuleValue(dto.ruleValue());
        entity.setDescription(dto.description());
        entity.setIsActive(dto.isActive());
}
}
