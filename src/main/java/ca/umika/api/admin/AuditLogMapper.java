package ca.umika.api.admin;

import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogDto toDto(AuditLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AuditLogDto(
                entity.getId(),
                entity.getUserId(),
                entity.getAction(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getBeforeState(),
                entity.getAfterState(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getCreatedAt()
        );
    }

    public AuditLogEntity toEntity(AuditLogDto dto) {
        AuditLogEntity entity = new AuditLogEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(AuditLogEntity entity, AuditLogDto dto) {
        entity.setUserId(dto.userId());
        entity.setAction(dto.action());
        entity.setEntityType(dto.entityType());
        entity.setEntityId(dto.entityId());
        entity.setBeforeState(dto.beforeState());
        entity.setAfterState(dto.afterState());
        entity.setIpAddress(dto.ipAddress());
        entity.setUserAgent(dto.userAgent());
        entity.setCreatedAt(dto.createdAt());
    }
}
