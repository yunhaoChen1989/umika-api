package ca.umika.api.store;

public record BusinessSettingItemDto(
        String settingGroup,
        String settingKey,
        String label,
        String description,
        String valueType,
        String unit,
        String systemValue,
        String locationValue,
        String effectiveValue,
        String source
) {
}
