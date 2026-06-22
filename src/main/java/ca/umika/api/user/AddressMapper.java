package ca.umika.api.user;

import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public AddressDto toDto(AddressEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AddressDto(
                entity.getId(),
                entity.getUserId(),
                entity.getLine1(),
                entity.getLine2(),
                entity.getCity(),
                entity.getProvince(),
                entity.getPostalCode(),
                entity.getCountry(),
                entity.getIsDefault(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public AddressEntity toEntity(AddressDto dto) {
        AddressEntity entity = new AddressEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(AddressEntity entity, AddressDto dto) {
        entity.setUserId(dto.userId());
        entity.setLine1(dto.line1());
        entity.setLine2(dto.line2());
        entity.setCity(dto.city());
        entity.setProvince(dto.province());
        entity.setPostalCode(dto.postalCode());
        entity.setCountry(dto.country());
        entity.setIsDefault(dto.isDefault());
        entity.setCreatedAt(dto.createdAt());
        entity.setUpdatedAt(dto.updatedAt());
    }
}
