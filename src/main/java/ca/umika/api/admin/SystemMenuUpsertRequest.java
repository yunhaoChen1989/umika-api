package ca.umika.api.admin;

import java.util.UUID;

public record SystemMenuUpsertRequest(
        UUID parentId,
        String name,
        String code,
        String path,
        String component,
        String icon,
        String menuType,
        Integer sortOrder,
        Boolean isVisible,
        Boolean isEnabled
) {
}
