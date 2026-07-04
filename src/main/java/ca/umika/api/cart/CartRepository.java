package ca.umika.api.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CartRepository extends JpaRepository<CartEntity, UUID> {
    List<CartEntity> findByUserIdAndLocationIdAndStatusOrderByCreatedAtDesc(UUID userId, UUID locationId, String status);
    List<CartEntity> findBySessionIdAndLocationIdAndStatusOrderByCreatedAtDesc(String sessionId, UUID locationId, String status);
}
