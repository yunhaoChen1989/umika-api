package ca.umika.api.store;

import java.util.List;
import java.util.UUID;

public record BusinessSettingsResponse(
        UUID locationId,
        String locationCode,
        String locationName,
        List<BusinessSettingItemDto> settings
) {
}
