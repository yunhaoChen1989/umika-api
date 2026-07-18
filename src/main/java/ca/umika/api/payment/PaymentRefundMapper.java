package ca.umika.api.payment;

import org.springframework.stereotype.Component;

@Component
public class PaymentRefundMapper {

    public PaymentRefundDto toDto(PaymentRefundEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentRefundDto(
                entity.getId(),
                entity.getPaymentTransactionId(),
                entity.getOrderId(),
                entity.getUserId(),
                entity.getRequestedBy(),
                entity.getAmount(),
                entity.getReason(),
                entity.getProviderRefundId(),
                entity.getIdempotencyKey(),
                entity.getFailureReason(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PaymentRefundEntity toEntity(PaymentRefundDto dto) {
        PaymentRefundEntity entity = new PaymentRefundEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(PaymentRefundEntity entity, PaymentRefundDto dto) {
        entity.setPaymentTransactionId(dto.paymentTransactionId());
        entity.setOrderId(dto.orderId());
        entity.setUserId(dto.userId());
        entity.setRequestedBy(dto.requestedBy());
        entity.setAmount(dto.amount());
        entity.setReason(dto.reason());
        entity.setProviderRefundId(dto.providerRefundId());
        entity.setIdempotencyKey(dto.idempotencyKey());
        entity.setFailureReason(dto.failureReason());
        entity.setStatus(dto.status());
}
}
