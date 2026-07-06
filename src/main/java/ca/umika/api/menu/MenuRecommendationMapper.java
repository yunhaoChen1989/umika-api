package ca.umika.api.menu;

import org.springframework.stereotype.Component;

@Component
public class MenuRecommendationMapper {

    public MenuRecommendationDto toDto(MenuRecommendationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new MenuRecommendationDto(
                entity.getId(),
                entity.getLocationId(),
                entity.getMenuItemId(),
                entity.getTitle(),
                entity.getSubtitle(),
                entity.getSortOrder(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MenuRecommendationEntity toEntity(MenuRecommendationDto dto) {
        MenuRecommendationEntity entity = new MenuRecommendationEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(MenuRecommendationEntity entity, MenuRecommendationDto dto) {
        entity.setLocationId(dto.locationId());
        entity.setMenuItemId(dto.menuItemId());
        entity.setTitle(dto.title());
        entity.setSubtitle(dto.subtitle());
        entity.setSortOrder(dto.sortOrder());
        entity.setIsActive(dto.isActive());
    }
}
