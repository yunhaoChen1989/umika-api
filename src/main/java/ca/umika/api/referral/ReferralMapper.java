package ca.umika.api.referral;

import org.springframework.stereotype.Component;

@Component
public class ReferralMapper {

    public ReferralDto toDto(ReferralEntity entity) {
        if (entity == null) {
            return null;
        }
        return new ReferralDto(
                entity.getId(),
                entity.getReferrerId(),
                entity.getReferredUserId(),
                entity.getStatus(),
                entity.getReferralCode(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ReferralEntity toEntity(ReferralDto dto) {
        ReferralEntity entity = new ReferralEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(ReferralEntity entity, ReferralDto dto) {
        entity.setReferrerId(dto.referrerId());
        entity.setReferredUserId(dto.referredUserId());
        entity.setStatus(dto.status());
        entity.setReferralCode(dto.referralCode());
}
}
