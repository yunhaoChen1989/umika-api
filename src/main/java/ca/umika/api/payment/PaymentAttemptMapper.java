package ca.umika.api.payment;

import org.springframework.stereotype.Component;

@Component
public class PaymentAttemptMapper {

    public PaymentAttemptDto toDto(PaymentAttemptEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentAttemptDto(
                entity.getId(),
                entity.getOrderId(),
                entity.getUserId(),
                entity.getAttemptNumber(),
                entity.getStatus(),
                entity.getRequestPayload(),
                entity.getResponsePayload(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }

    public PaymentAttemptEntity toEntity(PaymentAttemptDto dto) {
        PaymentAttemptEntity entity = new PaymentAttemptEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(PaymentAttemptEntity entity, PaymentAttemptDto dto) {
        entity.setOrderId(dto.orderId());
        entity.setUserId(dto.userId());
        entity.setAttemptNumber(dto.attemptNumber());
        entity.setStatus(dto.status());
        entity.setRequestPayload(dto.requestPayload());
        entity.setResponsePayload(dto.responsePayload());
        entity.setErrorMessage(dto.errorMessage());
}
}
