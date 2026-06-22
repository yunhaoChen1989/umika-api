package ca.umika.api.reward;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardTransactionRepository extends JpaRepository<RewardTransactionEntity, UUID> {
}
