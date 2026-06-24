package ca.umika.api.admin;

import org.springframework.stereotype.Component;

@Component
public class AdminActivityLogMapper {

    public AdminActivityLogDto toDto(AdminActivityLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AdminActivityLogDto(
                entity.getId(),
                entity.getUserId(),
                entity.getAction(),
                entity.getDetail(),
                entity.getCreatedAt()
        );
    }

    public AdminActivityLogEntity toEntity(AdminActivityLogDto dto) {
        AdminActivityLogEntity entity = new AdminActivityLogEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(AdminActivityLogEntity entity, AdminActivityLogDto dto) {
        entity.setUserId(dto.userId());
        entity.setAction(dto.action());
        entity.setDetail(dto.detail());
}
}
