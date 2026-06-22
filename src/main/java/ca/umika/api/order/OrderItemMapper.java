package ca.umika.api.order;

import org.springframework.stereotype.Component;

@Component
public class OrderItemMapper {

    public OrderItemDto toDto(OrderItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OrderItemDto(
                entity.getId(),
                entity.getOrderId(),
                entity.getMenuItemId(),
                entity.getItemName(),
                entity.getQuantity(),
                entity.getUnitPrice(),
                entity.getTotalPrice(),
                entity.getOptionSnapshot(),
                entity.getCreatedAt()
        );
    }

    public OrderItemEntity toEntity(OrderItemDto dto) {
        OrderItemEntity entity = new OrderItemEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(OrderItemEntity entity, OrderItemDto dto) {
        entity.setOrderId(dto.orderId());
        entity.setMenuItemId(dto.menuItemId());
        entity.setItemName(dto.itemName());
        entity.setQuantity(dto.quantity());
        entity.setUnitPrice(dto.unitPrice());
        entity.setTotalPrice(dto.totalPrice());
        entity.setOptionSnapshot(dto.optionSnapshot());
        entity.setCreatedAt(dto.createdAt());
    }
}
