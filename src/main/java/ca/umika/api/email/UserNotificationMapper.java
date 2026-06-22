package ca.umika.api.email;

import org.springframework.stereotype.Component;

@Component
public class UserNotificationMapper {

    public UserNotificationDto toDto(UserNotificationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserNotificationDto(
                entity.getId(),
                entity.getUserId(),
                entity.getNotificationId(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getIsRead(),
                entity.getReadAt(),
                entity.getCreatedAt()
        );
    }

    public UserNotificationEntity toEntity(UserNotificationDto dto) {
        UserNotificationEntity entity = new UserNotificationEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(UserNotificationEntity entity, UserNotificationDto dto) {
        entity.setUserId(dto.userId());
        entity.setNotificationId(dto.notificationId());
        entity.setTitle(dto.title());
        entity.setMessage(dto.message());
        entity.setIsRead(dto.isRead());
        entity.setReadAt(dto.readAt());
        entity.setCreatedAt(dto.createdAt());
    }
}
