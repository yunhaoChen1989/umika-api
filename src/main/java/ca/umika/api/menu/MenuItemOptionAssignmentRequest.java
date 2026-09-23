package ca.umika.api.menu;

import java.util.Set;
import java.util.UUID;

public record MenuItemOptionAssignmentRequest(Set<UUID> categoryIds) {
}

