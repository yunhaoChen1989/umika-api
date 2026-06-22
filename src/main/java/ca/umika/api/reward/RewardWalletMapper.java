package ca.umika.api.reward;

import org.springframework.stereotype.Component;

@Component
public class RewardWalletMapper {

    public RewardWalletDto toDto(RewardWalletEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RewardWalletDto(
                entity.getId(),
                entity.getUserId(),
                entity.getTotalEarned(),
                entity.getTotalRedeemed(),
                entity.getAvailableBalance(),
                entity.getUpdatedAt()
        );
    }

    public RewardWalletEntity toEntity(RewardWalletDto dto) {
        RewardWalletEntity entity = new RewardWalletEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(RewardWalletEntity entity, RewardWalletDto dto) {
        entity.setUserId(dto.userId());
        entity.setTotalEarned(dto.totalEarned());
        entity.setTotalRedeemed(dto.totalRedeemed());
        entity.setAvailableBalance(dto.availableBalance());
        entity.setUpdatedAt(dto.updatedAt());
    }
}
