package ca.umika.api.email;

import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateMapper {

    public NotificationTemplateDto toDto(NotificationTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        return new NotificationTemplateDto(
                entity.getId(),
                entity.getTemplateCode(),
                entity.getChannel(),
                entity.getSubject(),
                entity.getContent(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public NotificationTemplateEntity toEntity(NotificationTemplateDto dto) {
        NotificationTemplateEntity entity = new NotificationTemplateEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(NotificationTemplateEntity entity, NotificationTemplateDto dto) {
        entity.setTemplateCode(dto.templateCode());
        entity.setChannel(dto.channel());
        entity.setSubject(dto.subject());
        entity.setContent(dto.content());
        entity.setIsActive(dto.isActive());
}
}
