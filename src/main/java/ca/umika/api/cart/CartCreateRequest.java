package ca.umika.api.cart;

import java.util.UUID;

public record CartCreateRequest(
        UUID locationId,
        String sessionId
) {
}
