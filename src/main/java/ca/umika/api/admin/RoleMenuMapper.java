package ca.umika.api.admin;

import org.springframework.stereotype.Component;

@Component
public class RoleMenuMapper {

    public RoleMenuDto toDto(RoleMenuEntity entity) {
        if (entity == null || entity.getId() == null) { return null; }
        return new RoleMenuDto(entity.getId().getRoleId(), entity.getId().getMenuId());
    }

    public RoleMenuEntity toEntity(RoleMenuDto dto) {
        RoleMenuEntity entity = new RoleMenuEntity();
        entity.setId(new RoleMenuId(dto.roleId(), dto.menuId()));
        return entity;
    }
}
