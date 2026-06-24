package ca.umika.api.user;

import java.time.LocalDateTime;
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

    public UserEntity toEntity(UserWriteRequest dto) {
        UserEntity entity = new UserEntity();
        applyWriteRequest(entity, dto);
return entity;
    }

    public void updateEntity(UserEntity entity, UserWriteRequest dto) {
        applyWriteRequest(entity, dto);
}

    private void applyWriteRequest(UserEntity entity, UserWriteRequest dto) {
        if (dto.email() != null) {
            entity.setEmail(dto.email());
        }
        if (dto.phone() != null) {
            entity.setPhone(dto.phone());
        }
        if (dto.emailVerified() != null) {
            entity.setEmailVerified(dto.emailVerified());
        }
        if (dto.emailVerifiedAt() != null) {
            entity.setEmailVerifiedAt(dto.emailVerifiedAt());
        }
        if (dto.referralCode() != null) {
            entity.setReferralCode(dto.referralCode());
        }
        if (dto.referredBy() != null) {
            entity.setReferredBy(dto.referredBy());
        }
        if (dto.stripeCustomerId() != null) {
            entity.setStripeCustomerId(dto.stripeCustomerId());
        }
        if (dto.isActive() != null) {
            entity.setIsActive(dto.isActive());
        }
        if (dto.location() != null) {
            entity.setLocationId(dto.location());
        }
        if (dto.lastLoginAt() != null) {
            entity.setLastLoginAt(dto.lastLoginAt());
        }
        if (dto.lastLoginIp() != null) {
            entity.setLastLoginIp(dto.lastLoginIp());
        }
    }
}
