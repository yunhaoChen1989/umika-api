package ca.umika.api.store;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BusinessHourDto(
        UUID id,
        UUID locationId,
        Short dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        Boolean isClosed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
