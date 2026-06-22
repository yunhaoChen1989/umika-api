package ca.umika.api.email;

import org.springframework.stereotype.Component;

@Component
public class NotificationDeliveryLogMapper {

    public NotificationDeliveryLogDto toDto(NotificationDeliveryLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NotificationDeliveryLogDto(
                entity.getId(),
                entity.getNotificationId(),
                entity.getChannel(),
                entity.getAttemptStatus(),
                entity.getErrorMessage(),
                entity.getProviderResponse(),
                entity.getCreatedAt()
        );
    }

    public NotificationDeliveryLogEntity toEntity(NotificationDeliveryLogDto dto) {
        NotificationDeliveryLogEntity entity = new NotificationDeliveryLogEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(NotificationDeliveryLogEntity entity, NotificationDeliveryLogDto dto) {
        entity.setNotificationId(dto.notificationId());
        entity.setChannel(dto.channel());
        entity.setAttemptStatus(dto.attemptStatus());
        entity.setErrorMessage(dto.errorMessage());
        entity.setProviderResponse(dto.providerResponse());
        entity.setCreatedAt(dto.createdAt());
    }
}
