package ca.umika.api.admin;

import org.springframework.stereotype.Component;

@Component
public class SystemMenuMapper {

    public SystemMenuDto toDto(SystemMenuEntity entity) {
        if (entity == null) {
            return null;
        }
        return new SystemMenuDto(
                entity.getId(),
                entity.getParentId(),
                entity.getName(),
                entity.getCode(),
                entity.getPath(),
                entity.getComponent(),
                entity.getIcon(),
                entity.getMenuType(),
                entity.getSortOrder(),
                entity.getIsVisible(),
                entity.getIsEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public SystemMenuEntity toEntity(SystemMenuDto dto) {
        SystemMenuEntity entity = new SystemMenuEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(SystemMenuEntity entity, SystemMenuDto dto) {
        entity.setParentId(dto.parentId());
        entity.setName(dto.name());
        entity.setCode(dto.code());
        entity.setPath(dto.path());
        entity.setComponent(dto.component());
        entity.setIcon(dto.icon());
        entity.setMenuType(dto.menuType());
        entity.setSortOrder(dto.sortOrder());
        entity.setIsVisible(dto.isVisible());
        entity.setIsEnabled(dto.isEnabled());
        entity.setCreatedAt(dto.createdAt());
        entity.setUpdatedAt(dto.updatedAt());
    }
}
