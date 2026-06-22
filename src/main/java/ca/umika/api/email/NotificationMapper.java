package ca.umika.api.email;

import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationDto toDto(NotificationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NotificationDto(
                entity.getId(),
                entity.getUserId(),
                entity.getType(),
                entity.getTemplateCode(),
                entity.getChannel(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getData(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getScheduledAt(),
                entity.getSentAt(),
                entity.getCreatedAt()
        );
    }

    public NotificationEntity toEntity(NotificationDto dto) {
        NotificationEntity entity = new NotificationEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(NotificationEntity entity, NotificationDto dto) {
        entity.setUserId(dto.userId());
        entity.setType(dto.type());
        entity.setTemplateCode(dto.templateCode());
        entity.setChannel(dto.channel());
        entity.setTitle(dto.title());
        entity.setMessage(dto.message());
        entity.setData(dto.data());
        entity.setStatus(dto.status());
        entity.setRetryCount(dto.retryCount());
        entity.setScheduledAt(dto.scheduledAt());
        entity.setSentAt(dto.sentAt());
        entity.setCreatedAt(dto.createdAt());
    }
}
