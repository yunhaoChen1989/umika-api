package ca.umika.api.admin;

import org.springframework.stereotype.Component;

@Component
public class UserPermissionMapper {

    public UserPermissionDto toDto(UserPermissionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserPermissionDto(
                entity.getId(),
                entity.getUserId(),
                entity.getPermissionCode(),
                entity.getIsGranted(),
                entity.getLocationId(),
                entity.getCreatedAt()
        );
    }

    public UserPermissionEntity toEntity(UserPermissionDto dto) {
        UserPermissionEntity entity = new UserPermissionEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(UserPermissionEntity entity, UserPermissionDto dto) {
        entity.setUserId(dto.userId());
        entity.setPermissionCode(dto.permissionCode());
        entity.setIsGranted(dto.isGranted());
        entity.setLocationId(dto.locationId());
}
}
