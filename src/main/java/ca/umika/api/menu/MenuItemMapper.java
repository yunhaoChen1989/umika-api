package ca.umika.api.menu;

import org.springframework.stereotype.Component;

@Component
public class MenuItemMapper {

    public MenuItemDto toDto(MenuItemEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MenuItemDto(
                entity.getId(),
                entity.getCategoryId(),
                entity.getLocationId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getSku(),
                entity.getDisplayOrder(),
                entity.getIsAvailable(),
                entity.getIsActive(),
                entity.getIsDeleted(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MenuItemEntity toEntity(MenuItemDto dto) {
        MenuItemEntity entity = new MenuItemEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(MenuItemEntity entity, MenuItemDto dto) {
        entity.setCategoryId(dto.categoryId());
        entity.setLocationId(dto.locationId());
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setPrice(dto.price());
        entity.setSku(dto.sku());
        entity.setDisplayOrder(dto.displayOrder());
        entity.setIsAvailable(dto.isAvailable());
        entity.setIsActive(dto.isActive());
        entity.setIsDeleted(dto.isDeleted());
        entity.setCreatedAt(dto.createdAt());
        entity.setUpdatedAt(dto.updatedAt());
    }
}
