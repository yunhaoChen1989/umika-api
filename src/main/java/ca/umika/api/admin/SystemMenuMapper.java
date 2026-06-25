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
        apply(entity, dto.parentId(), dto.name(), dto.code(), dto.path(), dto.component(), dto.icon(), dto.menuType(), dto.sortOrder(), dto.isVisible(), dto.isEnabled());
    }

    public SystemMenuEntity toEntity(SystemMenuUpsertRequest request) {
        SystemMenuEntity entity = new SystemMenuEntity();
        updateEntity(entity, request);
        return entity;
    }

    public void updateEntity(SystemMenuEntity entity, SystemMenuUpsertRequest request) {
        apply(entity, request.parentId(), request.name(), request.code(), request.path(), request.component(), request.icon(), request.menuType(), request.sortOrder(), request.isVisible(), request.isEnabled());
    }

    private void apply(
            SystemMenuEntity entity,
            java.util.UUID parentId,
            String name,
            String code,
            String path,
            String component,
            String icon,
            String menuType,
            Integer sortOrder,
            Boolean isVisible,
            Boolean isEnabled
    ) {
        entity.setParentId(parentId);
        entity.setName(name);
        entity.setCode(code);
        entity.setPath(path);
        entity.setComponent(component);
        entity.setIcon(icon);
        entity.setMenuType(menuType);
        entity.setSortOrder(sortOrder);
        entity.setIsVisible(isVisible);
        entity.setIsEnabled(isEnabled);
    }
}
