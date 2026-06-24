package ca.umika.api.order;

import org.springframework.stereotype.Component;

@Component
public class OrderTaxMapper {

    public OrderTaxDto toDto(OrderTaxEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OrderTaxDto(
                entity.getId(),
                entity.getOrderId(),
                entity.getTaxRuleId(),
                entity.getTaxName(),
                entity.getTaxRate(),
                entity.getTaxableAmount(),
                entity.getTaxAmount(),
                entity.getCreatedAt()
        );
    }

    public OrderTaxEntity toEntity(OrderTaxDto dto) {
        OrderTaxEntity entity = new OrderTaxEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(OrderTaxEntity entity, OrderTaxDto dto) {
        entity.setOrderId(dto.orderId());
        entity.setTaxRuleId(dto.taxRuleId());
        entity.setTaxName(dto.taxName());
        entity.setTaxRate(dto.taxRate());
        entity.setTaxableAmount(dto.taxableAmount());
        entity.setTaxAmount(dto.taxAmount());
}
}
