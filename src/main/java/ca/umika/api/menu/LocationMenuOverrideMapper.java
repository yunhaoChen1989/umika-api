package ca.umika.api.menu;

import org.springframework.stereotype.Component;

@Component
public class LocationMenuOverrideMapper {

    public LocationMenuOverrideDto toDto(LocationMenuOverrideEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LocationMenuOverrideDto(
                entity.getId(),
                entity.getLocationId(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getIsVisible(),
                entity.getSortOrder(),
                entity.getCustomName(),
                entity.getCustomDescription(),
                entity.getCustomPrice(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public LocationMenuOverrideEntity toEntity(LocationMenuOverrideDto dto) {
        LocationMenuOverrideEntity entity = new LocationMenuOverrideEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(LocationMenuOverrideEntity entity, LocationMenuOverrideDto dto) {
        entity.setLocationId(dto.locationId());
        entity.setTargetType(normalizeTargetType(dto.targetType()));
        entity.setTargetId(dto.targetId());
        entity.setIsVisible(dto.isVisible());
        entity.setSortOrder(dto.sortOrder());
        entity.setCustomName(dto.customName());
        entity.setCustomDescription(dto.customDescription());
        entity.setCustomPrice(dto.customPrice());
    }

    private String normalizeTargetType(String targetType) {
        return targetType == null ? null : targetType.trim().toUpperCase();
    }
}
