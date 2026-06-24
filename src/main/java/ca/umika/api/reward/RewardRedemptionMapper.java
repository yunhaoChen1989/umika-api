package ca.umika.api.reward;

import org.springframework.stereotype.Component;

@Component
public class RewardRedemptionMapper {

    public RewardRedemptionDto toDto(RewardRedemptionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RewardRedemptionDto(
                entity.getId(),
                entity.getUserId(),
                entity.getOrderId(),
                entity.getPointsRedeemed(),
                entity.getCashValue(),
                entity.getCreatedAt()
        );
    }

    public RewardRedemptionEntity toEntity(RewardRedemptionDto dto) {
        RewardRedemptionEntity entity = new RewardRedemptionEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(RewardRedemptionEntity entity, RewardRedemptionDto dto) {
        entity.setUserId(dto.userId());
        entity.setOrderId(dto.orderId());
        entity.setPointsRedeemed(dto.pointsRedeemed());
        entity.setCashValue(dto.cashValue());
}
}
