package ca.umika.api.auth;

import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {

    public RefreshTokenDto toDto(RefreshTokenEntity entity) {
        if (entity == null) {
            return null;
        }
        return new RefreshTokenDto(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getRevoked(),
                entity.getCreatedAt()
        );
    }

    public RefreshTokenEntity toEntity(RefreshTokenDto dto) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(RefreshTokenEntity entity, RefreshTokenDto dto) {
        entity.setUserId(dto.userId());
        entity.setTokenHash(dto.tokenHash());
        entity.setExpiresAt(dto.expiresAt());
        entity.setRevoked(dto.revoked());
        entity.setCreatedAt(dto.createdAt());
    }
}
