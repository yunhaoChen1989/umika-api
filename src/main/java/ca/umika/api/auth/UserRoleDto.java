package ca.umika.api.auth;

import java.util.UUID;

public record UserRoleDto(
        UUID userId,
        UUID roleId
) {
}
