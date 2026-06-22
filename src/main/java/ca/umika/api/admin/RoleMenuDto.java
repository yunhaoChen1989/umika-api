package ca.umika.api.admin;

import java.util.UUID;

public record RoleMenuDto(
        UUID roleId,
        UUID menuId
) {
}
