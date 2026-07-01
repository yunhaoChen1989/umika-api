package ca.umika.api.store;

import java.util.List;

public record BusinessSettingsUpdateRequest(
        List<BusinessSettingValueRequest> settings
) {
}
