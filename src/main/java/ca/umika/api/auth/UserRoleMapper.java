package ca.umika.api.auth;

import org.springframework.stereotype.Component;

@Component
public class UserRoleMapper {

    public UserRoleDto toDto(UserRoleEntity entity) {
        if (entity == null || entity.getId() == null) { return null; }
        return new UserRoleDto(entity.getId().getUserId(), entity.getId().getRoleId());
    }

    public UserRoleEntity toEntity(UserRoleDto dto) {
        UserRoleEntity entity = new UserRoleEntity();
        entity.setId(new UserRoleId(dto.userId(), dto.roleId()));
        return entity;
    }
}
