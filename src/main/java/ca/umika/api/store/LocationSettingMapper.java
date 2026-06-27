package ca.umika.api.store;

import org.springframework.stereotype.Component;

@Component
public class LocationSettingMapper {

    public LocationSettingDto toDto(LocationSettingEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LocationSettingDto(
                entity.getId(),
                entity.getLocationId(),
                entity.getSettingGroup(),
                entity.getSettingKey(),
                entity.getSettingValue(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public LocationSettingEntity toEntity(LocationSettingDto dto) {
        LocationSettingEntity entity = new LocationSettingEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(LocationSettingEntity entity, LocationSettingDto dto) {
        entity.setLocationId(dto.locationId());
        entity.setSettingGroup(normalizeSettingGroup(dto.settingGroup()));
        entity.setSettingKey(dto.settingKey());
        entity.setSettingValue(dto.settingValue());
        entity.setDescription(dto.description());
    }

    private String normalizeSettingGroup(String settingGroup) {
        if (settingGroup == null || settingGroup.isBlank()) {
            return "GENERAL";
        }
        return settingGroup.trim().toUpperCase();
    }
}
