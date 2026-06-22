package ca.umika.api.payment;

import org.springframework.stereotype.Component;

@Component
public class PaymentWebhookLogMapper {

    public PaymentWebhookLogDto toDto(PaymentWebhookLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentWebhookLogDto(
                entity.getId(),
                entity.getProvider(),
                entity.getEventType(),
                entity.getProviderEventId(),
                entity.getPayload(),
                entity.getProcessed(),
                entity.getProcessingError(),
                entity.getCreatedAt()
        );
    }

    public PaymentWebhookLogEntity toEntity(PaymentWebhookLogDto dto) {
        PaymentWebhookLogEntity entity = new PaymentWebhookLogEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(PaymentWebhookLogEntity entity, PaymentWebhookLogDto dto) {
        entity.setProvider(dto.provider());
        entity.setEventType(dto.eventType());
        entity.setProviderEventId(dto.providerEventId());
        entity.setPayload(dto.payload());
        entity.setProcessed(dto.processed());
        entity.setProcessingError(dto.processingError());
        entity.setCreatedAt(dto.createdAt());
    }
}
