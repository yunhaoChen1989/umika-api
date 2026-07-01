package ca.umika.api.admin;

import org.springframework.stereotype.Component;

@Component
public class PermissionCodeMapper {

    public PermissionCodeDto toDto(PermissionCodeEntity entity) {
        return new PermissionCodeDto(
                entity.getId(),
                entity.getPermissionGroup(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
