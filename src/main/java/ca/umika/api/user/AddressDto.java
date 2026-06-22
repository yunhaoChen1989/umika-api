package ca.umika.api.user;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record AddressDto(
        UUID id,
        UUID userId,
        String line1,
        String line2,
        String city,
        String province,
        String postalCode,
        String country,
        Boolean isDefault,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
