package ca.umika.api.reward;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardWalletRepository extends JpaRepository<RewardWalletEntity, UUID> {
    Optional<RewardWalletEntity> findByUserId(UUID userId);
}
