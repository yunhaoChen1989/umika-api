package ca.umika.api.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CartRepository extends JpaRepository<CartEntity, UUID> {
    Optional<CartEntity> findByUserIdAndLocationIdAndStatus(UUID userId, UUID locationId, String status);
    Optional<CartEntity> findBySessionIdAndLocationIdAndStatus(String sessionId, UUID locationId, String status);
}
