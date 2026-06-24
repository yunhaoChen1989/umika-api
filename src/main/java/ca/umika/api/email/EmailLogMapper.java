package ca.umika.api.email;

import org.springframework.stereotype.Component;

@Component
public class EmailLogMapper {

    public EmailLogDto toDto(EmailLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return new EmailLogDto(
                entity.getId(),
                entity.getUserId(),
                entity.getNotificationId(),
                entity.getRecipientEmail(),
                entity.getSubject(),
                entity.getStatus(),
                entity.getProvider(),
                entity.getProviderMessageId(),
                entity.getErrorMessage(),
                entity.getSentAt(),
                entity.getCreatedAt()
        );
    }

    public EmailLogEntity toEntity(EmailLogDto dto) {
        EmailLogEntity entity = new EmailLogEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(EmailLogEntity entity, EmailLogDto dto) {
        entity.setUserId(dto.userId());
        entity.setNotificationId(dto.notificationId());
        entity.setRecipientEmail(dto.recipientEmail());
        entity.setSubject(dto.subject());
        entity.setStatus(dto.status());
        entity.setProvider(dto.provider());
        entity.setProviderMessageId(dto.providerMessageId());
        entity.setErrorMessage(dto.errorMessage());
        entity.setSentAt(dto.sentAt());
}
}
