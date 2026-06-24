package ca.umika.api.reward;

import org.springframework.stereotype.Component;

@Component
public class RewardTransactionMapper {

    public RewardTransactionDto toDto(RewardTransactionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RewardTransactionDto(
                entity.getId(),
                entity.getUserId(),
                entity.getOrderId(),
                entity.getType(),
                entity.getPoints(),
                entity.getSource(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    public RewardTransactionEntity toEntity(RewardTransactionDto dto) {
        RewardTransactionEntity entity = new RewardTransactionEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(RewardTransactionEntity entity, RewardTransactionDto dto) {
        entity.setUserId(dto.userId());
        entity.setOrderId(dto.orderId());
        entity.setType(dto.type());
        entity.setPoints(dto.points());
        entity.setSource(dto.source());
        entity.setDescription(dto.description());
}
}
