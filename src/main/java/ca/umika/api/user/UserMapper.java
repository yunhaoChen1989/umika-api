package ca.umika.api.user;

import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDto(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserDto(
                entity.getId(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getPasswordHash(),
                entity.getEmailVerified(),
                entity.getEmailVerifiedAt(),
                entity.getReferralCode(),
                entity.getReferredBy(),
                entity.getStripeCustomerId(),
                entity.getIsActive(),
                entity.getLocationId(),
                entity.getLastLoginAt(),
                entity.getLastLoginIp(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserEntity toEntity(UserDto dto) {
        UserEntity entity = new UserEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(UserEntity entity, UserDto dto) {
        entity.setEmail(dto.email());
        entity.setPhone(dto.phone());
        entity.setPasswordHash(dto.passwordHash());
        entity.setEmailVerified(dto.emailVerified());
        entity.setEmailVerifiedAt(dto.emailVerifiedAt());
        entity.setReferralCode(dto.referralCode());
        entity.setReferredBy(dto.referredBy());
        entity.setStripeCustomerId(dto.stripeCustomerId());
        entity.setIsActive(dto.isActive());
        entity.setLocationId(dto.location());
        entity.setLastLoginAt(dto.lastLoginAt());
        entity.setLastLoginIp(dto.lastLoginIp());
        entity.setCreatedAt(dto.createdAt());
        entity.setUpdatedAt(dto.updatedAt());
    }
}
