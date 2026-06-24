package ca.umika.api.admin;

import org.springframework.stereotype.Component;

@Component
public class SystemSettingMapper {

    public SystemSettingDto toDto(SystemSettingEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SystemSettingDto(
                entity.getId(),
                entity.getSettingKey(),
                entity.getSettingValue(),
                entity.getDescription(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public SystemSettingEntity toEntity(SystemSettingDto dto) {
        SystemSettingEntity entity = new SystemSettingEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(SystemSettingEntity entity, SystemSettingDto dto) {
        entity.setSettingKey(dto.settingKey());
        entity.setSettingValue(dto.settingValue());
        entity.setDescription(dto.description());
}
}
