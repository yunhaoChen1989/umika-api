package ca.umika.api.order;

import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderDto toDto(OrderEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OrderDto(
                entity.getId(),
                entity.getUserId(),
                entity.getLocationId(),
                entity.getAddressId(),
                entity.getOrderNumber(),
                entity.getOrderType(),
                entity.getStatus(),
                entity.getSubtotal(),
                entity.getTotalDiscount(),
                entity.getTaxRate(),
                entity.getTaxAmount(),
                entity.getTipAmount(),
                entity.getFinalTotal(),
                entity.getCustomerNote(),
                entity.getInternalNote(),
                entity.getPromotionId(),
                entity.getCouponId(),
                entity.getTaxRuleId(),
                entity.getTaxExempt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public OrderEntity toEntity(OrderDto dto) {
        OrderEntity entity = new OrderEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(OrderEntity entity, OrderDto dto) {
        entity.setUserId(dto.userId());
        entity.setLocationId(dto.locationId());
        entity.setAddressId(dto.addressId());
        entity.setOrderNumber(dto.orderNumber());
        entity.setOrderType(dto.orderType());
        entity.setStatus(dto.status());
        entity.setSubtotal(dto.subtotal());
        entity.setTotalDiscount(dto.totalDiscount());
        entity.setTaxRate(dto.taxRate());
        entity.setTaxAmount(dto.taxAmount());
        entity.setTipAmount(dto.tipAmount());
        entity.setFinalTotal(dto.finalTotal());
        entity.setCustomerNote(dto.customerNote());
        entity.setInternalNote(dto.internalNote());
        entity.setPromotionId(dto.promotionId());
        entity.setCouponId(dto.couponId());
        entity.setTaxRuleId(dto.taxRuleId());
        entity.setTaxExempt(dto.taxExempt());
}
}
