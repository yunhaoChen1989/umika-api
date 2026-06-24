package ca.umika.api.menu;

import org.springframework.stereotype.Component;

@Component
public class MenuCategoryMapper {

    public MenuCategoryDto toDto(MenuCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MenuCategoryDto(
                entity.getId(),
                entity.getLocationId(),
                entity.getName(),
                entity.getDescription(),
                entity.getSortOrder(),
                entity.getIsActive(),
                entity.getIsDeleted(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MenuCategoryEntity toEntity(MenuCategoryDto dto) {
        MenuCategoryEntity entity = new MenuCategoryEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(MenuCategoryEntity entity, MenuCategoryDto dto) {
        entity.setLocationId(dto.locationId());
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setSortOrder(dto.sortOrder());
        entity.setIsActive(dto.isActive());
        entity.setIsDeleted(dto.isDeleted());
}
}
