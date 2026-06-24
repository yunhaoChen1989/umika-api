package ca.umika.api.store;

import org.springframework.stereotype.Component;

@Component
public class BusinessHourMapper {

    public BusinessHourDto toDto(BusinessHourEntity entity) {
        if (entity == null) {
            return null;
        }
        return new BusinessHourDto(
                entity.getId(),
                entity.getLocationId(),
                entity.getDayOfWeek(),
                entity.getOpenTime(),
                entity.getCloseTime(),
                entity.getIsClosed(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public BusinessHourEntity toEntity(BusinessHourDto dto) {
        BusinessHourEntity entity = new BusinessHourEntity();
        updateEntity(entity, dto);
        return entity;
    }

    public void updateEntity(BusinessHourEntity entity, BusinessHourDto dto) {
        entity.setLocationId(dto.locationId());
        entity.setDayOfWeek(dto.dayOfWeek());
        entity.setOpenTime(dto.openTime());
        entity.setCloseTime(dto.closeTime());
        entity.setIsClosed(dto.isClosed());
}
}
