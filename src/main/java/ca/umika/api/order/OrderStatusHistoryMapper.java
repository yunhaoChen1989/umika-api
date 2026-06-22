package ca.umika.api.order;

import org.springframework.stereotype.Component;

@Component
public class OrderStatusHistoryMapper {

    public OrderStatusHistoryDto toDto(OrderStatusHistoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OrderStatusHistoryDto(
                entity.getId(),
                entity.getOrderId(),
                entity.getOldStatus(),
                entity.getNewStatus(),
                entity.getChangedBy(),
                entity.getNote(),
                entity.getCreatedAt()
        );
    }

    public OrderStatusHistoryEntity toEntity(OrderStatusHistoryDto dto) {
        OrderStatusHistoryEntity entity = new OrderStatusHistoryEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(OrderStatusHistoryEntity entity, OrderStatusHistoryDto dto) {
        entity.setOrderId(dto.orderId());
        entity.setOldStatus(dto.oldStatus());
        entity.setNewStatus(dto.newStatus());
        entity.setChangedBy(dto.changedBy());
        entity.setNote(dto.note());
        entity.setCreatedAt(dto.createdAt());
    }
}
