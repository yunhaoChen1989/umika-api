package ca.umika.api.admin;

import org.springframework.stereotype.Component;

@Component
public class SystemConfigCacheMapper {

    public SystemConfigCacheDto toDto(SystemConfigCacheEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SystemConfigCacheDto(
                entity.getId(),
                entity.getConfigKey(),
                entity.getConfigValue(),
                entity.getUpdatedAt()
        );
    }

    public SystemConfigCacheEntity toEntity(SystemConfigCacheDto dto) {
        SystemConfigCacheEntity entity = new SystemConfigCacheEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(SystemConfigCacheEntity entity, SystemConfigCacheDto dto) {
        entity.setConfigKey(dto.configKey());
        entity.setConfigValue(dto.configValue());
}
}
