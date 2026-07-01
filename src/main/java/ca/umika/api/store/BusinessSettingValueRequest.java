package ca.umika.api.store;

public record BusinessSettingValueRequest(
        String settingKey,
        String settingValue
) {
}
