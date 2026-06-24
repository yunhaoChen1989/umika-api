package ca.umika.api.payment;

import org.springframework.stereotype.Component;

@Component
public class PaymentTransactionMapper {

    public PaymentTransactionDto toDto(PaymentTransactionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PaymentTransactionDto(
                entity.getId(),
                entity.getOrderId(),
                entity.getUserId(),
                entity.getProvider(),
                entity.getProviderPaymentId(),
                entity.getProviderIntentId(),
                entity.getStatus(),
                entity.getAmount(),
                entity.getCurrency(),
                entity.getPaymentMethod(),
                entity.getFailureReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public PaymentTransactionEntity toEntity(PaymentTransactionDto dto) {
        PaymentTransactionEntity entity = new PaymentTransactionEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(PaymentTransactionEntity entity, PaymentTransactionDto dto) {
        entity.setOrderId(dto.orderId());
        entity.setUserId(dto.userId());
        entity.setProvider(dto.provider());
        entity.setProviderPaymentId(dto.providerPaymentId());
        entity.setProviderIntentId(dto.providerIntentId());
        entity.setStatus(dto.status());
        entity.setAmount(dto.amount());
        entity.setCurrency(dto.currency());
        entity.setPaymentMethod(dto.paymentMethod());
        entity.setFailureReason(dto.failureReason());
}
}
