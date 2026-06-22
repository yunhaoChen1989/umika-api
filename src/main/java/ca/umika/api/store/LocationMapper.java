package ca.umika.api.store;

import org.springframework.stereotype.Component;

@Component
public class LocationMapper {

    public LocationDto toDto(LocationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new LocationDto(
                entity.getId(),
                entity.getName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAddressLine1(),
                entity.getAddressLine2(),
                entity.getCity(),
                entity.getProvince(),
                entity.getPostalCode(),
                entity.getCountry(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public LocationEntity toEntity(LocationDto dto) {
        LocationEntity entity = new LocationEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(LocationEntity entity, LocationDto dto) {
        entity.setName(dto.name());
        entity.setPhone(dto.phone());
        entity.setEmail(dto.email());
        entity.setAddressLine1(dto.addressLine1());
        entity.setAddressLine2(dto.addressLine2());
        entity.setCity(dto.city());
        entity.setProvince(dto.province());
        entity.setPostalCode(dto.postalCode());
        entity.setCountry(dto.country());
        entity.setIsActive(dto.isActive());
        entity.setCreatedAt(dto.createdAt());
        entity.setUpdatedAt(dto.updatedAt());
    }
}
