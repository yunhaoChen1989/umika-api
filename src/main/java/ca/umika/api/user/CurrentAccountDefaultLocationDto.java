package ca.umika.api.user;

import java.util.UUID;

public record CurrentAccountDefaultLocationDto(
        UUID locationId,
        String locationCode,
        String name,
        String addressLine1,
        String addressLine2,
        String city,
        String province,
        String postalCode,
        String country
) {
}
