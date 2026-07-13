package ca.umika.api.coupon;

import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public CouponDto toDto(CouponEntity entity) {
        return new CouponDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getDiscountType(),
                entity.getDiscountValue(),
                entity.getMinimumOrderAmount(),
                entity.getMaximumDiscountAmount(),
                entity.getLocationId(),
                entity.getStartsAt(),
                entity.getEndsAt(),
                entity.getUsageLimitTotal(),
                entity.getUsageLimitPerUser(),
                entity.getFirstOrderOnly(),
                entity.getNewCustomerOnly(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CouponEntity toEntity(CouponDto dto) {
        CouponEntity entity = new CouponEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(CouponEntity entity, CouponDto dto) {
        entity.setCode(dto.code());
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setDiscountType(dto.discountType());
        entity.setDiscountValue(dto.discountValue());
        entity.setMinimumOrderAmount(dto.minimumOrderAmount());
        entity.setMaximumDiscountAmount(dto.maximumDiscountAmount());
        entity.setLocationId(dto.locationId());
        entity.setStartsAt(dto.startsAt());
        entity.setEndsAt(dto.endsAt());
        entity.setUsageLimitTotal(dto.usageLimitTotal());
        entity.setUsageLimitPerUser(dto.usageLimitPerUser());
        entity.setFirstOrderOnly(dto.firstOrderOnly());
        entity.setNewCustomerOnly(dto.newCustomerOnly());
        entity.setIsActive(dto.isActive());
    }
}
