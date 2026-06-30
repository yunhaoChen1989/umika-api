package ca.umika.api.reward;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardRedemptionRepository extends JpaRepository<RewardRedemptionEntity, UUID> {
    Optional<RewardRedemptionEntity> findByOrderId(UUID orderId);
}
