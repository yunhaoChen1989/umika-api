package ca.umika.api.email;

import org.springframework.stereotype.Component;

@Component
public class NotificationPreferenceMapper {

    public NotificationPreferenceDto toDto(NotificationPreferenceEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NotificationPreferenceDto(
                entity.getId(),
                entity.getUserId(),
                entity.getEmailEnabled(),
                entity.getSmsEnabled(),
                entity.getMarketingEnabled(),
                entity.getOrderUpdatesEnabled(),
                entity.getReferralUpdatesEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public NotificationPreferenceEntity toEntity(NotificationPreferenceDto dto) {
        NotificationPreferenceEntity entity = new NotificationPreferenceEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(NotificationPreferenceEntity entity, NotificationPreferenceDto dto) {
        entity.setUserId(dto.userId());
        entity.setEmailEnabled(dto.emailEnabled());
        entity.setSmsEnabled(dto.smsEnabled());
        entity.setMarketingEnabled(dto.marketingEnabled());
        entity.setOrderUpdatesEnabled(dto.orderUpdatesEnabled());
        entity.setReferralUpdatesEnabled(dto.referralUpdatesEnabled());
}
}
