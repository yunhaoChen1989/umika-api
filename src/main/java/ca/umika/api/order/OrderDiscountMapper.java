package ca.umika.api.order;

import org.springframework.stereotype.Component;

@Component
public class OrderDiscountMapper {

    public OrderDiscountDto toDto(OrderDiscountEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OrderDiscountDto(
                entity.getId(),
                entity.getOrderId(),
                entity.getDiscountType(),
                entity.getReferenceId(),
                entity.getAmount(),
                entity.getMetadata(),
                entity.getCreatedAt()
        );
    }

    public OrderDiscountEntity toEntity(OrderDiscountDto dto) {
        OrderDiscountEntity entity = new OrderDiscountEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(OrderDiscountEntity entity, OrderDiscountDto dto) {
        entity.setOrderId(dto.orderId());
        entity.setDiscountType(dto.discountType());
        entity.setReferenceId(dto.referenceId());
        entity.setAmount(dto.amount());
        entity.setMetadata(dto.metadata());
}
}
