package ca.umika.api.store;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record LocationDto(
        UUID id,
        String locationCode,
        String name,
        String phone,
        String email,
        String addressLine1,
        String addressLine2,
        String city,
        String province,
        String postalCode,
        String country,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
