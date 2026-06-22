package ca.umika.api.auth;

import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public RoleDto toDto(RoleEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RoleDto(
                entity.getId(),
                entity.getName(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public RoleEntity toEntity(RoleDto dto) {
        RoleEntity entity = new RoleEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(RoleEntity entity, RoleDto dto) {
        entity.setName(dto.name());
        entity.setCreatedAt(dto.createdAt());
        entity.setUpdatedAt(dto.updatedAt());
    }
}
