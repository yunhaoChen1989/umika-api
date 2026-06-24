package ca.umika.api.user;

import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

    public UserProfileDto toDto(UserProfileEntity entity) {
        if (entity == null) {
            return null;
        }
        return new UserProfileDto(
                entity.getId(),
                entity.getUserId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getBirthday(),
                entity.getAvatarUrl(),
                entity.getPreferredLanguage(),
                entity.getMarketingEmailEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserProfileEntity toEntity(UserProfileDto dto) {
        UserProfileEntity entity = new UserProfileEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(UserProfileEntity entity, UserProfileDto dto) {
        entity.setUserId(dto.userId());
        entity.setFirstName(dto.firstName());
        entity.setLastName(dto.lastName());
        entity.setBirthday(dto.birthday());
        entity.setAvatarUrl(dto.avatarUrl());
        entity.setPreferredLanguage(dto.preferredLanguage());
        entity.setMarketingEmailEnabled(dto.marketingEmailEnabled());
}
}
