package ca.umika.api.admin;

import org.springframework.stereotype.Component;

@Component
public class RolePermissionMapper {

    public RolePermissionDto toDto(RolePermissionEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RolePermissionDto(
                entity.getId(),
                entity.getRoleId(),
                entity.getPermissionCode(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }

    public RolePermissionEntity toEntity(RolePermissionDto dto) {
        RolePermissionEntity entity = new RolePermissionEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(RolePermissionEntity entity, RolePermissionDto dto) {
        entity.setRoleId(dto.roleId());
        entity.setPermissionCode(dto.permissionCode());
        entity.setDescription(dto.description());
}
}
