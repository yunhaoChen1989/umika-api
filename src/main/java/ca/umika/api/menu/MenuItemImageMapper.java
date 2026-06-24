package ca.umika.api.menu;

import org.springframework.stereotype.Component;

@Component
public class MenuItemImageMapper {

    public MenuItemImageDto toDto(MenuItemImageEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MenuItemImageDto(
                entity.getId(),
                entity.getMenuItemId(),
                entity.getImageUrl(),
                entity.getIsPrimary(),
                entity.getSortOrder(),
                entity.getCreatedAt()
        );
    }

    public MenuItemImageEntity toEntity(MenuItemImageDto dto) {
        MenuItemImageEntity entity = new MenuItemImageEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(MenuItemImageEntity entity, MenuItemImageDto dto) {
        entity.setMenuItemId(dto.menuItemId());
        entity.setImageUrl(dto.imageUrl());
        entity.setIsPrimary(dto.isPrimary());
        entity.setSortOrder(dto.sortOrder());
}
}
