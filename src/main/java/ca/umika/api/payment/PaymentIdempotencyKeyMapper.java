package ca.umika.api.payment;

import org.springframework.stereotype.Component;

@Component
public class PaymentIdempotencyKeyMapper {

    public PaymentIdempotencyKeyDto toDto(PaymentIdempotencyKeyEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentIdempotencyKeyDto(
                entity.getId(),
                entity.getUserId(),
                entity.getIdempotencyKey(),
                entity.getRequestType(),
                entity.getRequestHash(),
                entity.getResponseData(),
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }

    public PaymentIdempotencyKeyEntity toEntity(PaymentIdempotencyKeyDto dto) {
        PaymentIdempotencyKeyEntity entity = new PaymentIdempotencyKeyEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(PaymentIdempotencyKeyEntity entity, PaymentIdempotencyKeyDto dto) {
        entity.setUserId(dto.userId());
        entity.setIdempotencyKey(dto.idempotencyKey());
        entity.setRequestType(dto.requestType());
        entity.setRequestHash(dto.requestHash());
        entity.setResponseData(dto.responseData());
        entity.setExpiresAt(dto.expiresAt());
        entity.setCreatedAt(dto.createdAt());
    }
}
