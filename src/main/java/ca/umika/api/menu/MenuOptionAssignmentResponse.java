package ca.umika.api.menu;

import java.util.Set;
import java.util.UUID;

public record MenuOptionAssignmentResponse(
        boolean customized,
        Set<UUID> categoryIds,
        Set<UUID> effectiveCategoryIds
) {
}
