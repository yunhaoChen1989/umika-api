package ca.umika.api.menu;

import org.springframework.stereotype.Component;

@Component
public class MenuItemOptionMapper {

    public MenuItemOptionDto toDto(MenuItemOptionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MenuItemOptionDto(
                entity.getId(),
                entity.getItemId(),
                entity.getName(),
                entity.getPriceModifier(),
                entity.getIsRequired(),
                entity.getSortOrder(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MenuItemOptionEntity toEntity(MenuItemOptionDto dto) {
        MenuItemOptionEntity entity = new MenuItemOptionEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(MenuItemOptionEntity entity, MenuItemOptionDto dto) {
        entity.setItemId(dto.itemId());
        entity.setName(dto.name());
        entity.setPriceModifier(dto.priceModifier());
        entity.setIsRequired(dto.isRequired());
        entity.setSortOrder(dto.sortOrder());
        entity.setIsActive(dto.isActive());
        entity.setCreatedAt(dto.createdAt());
        entity.setUpdatedAt(dto.updatedAt());
    }
}
