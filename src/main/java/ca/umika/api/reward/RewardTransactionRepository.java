package ca.umika.api.reward;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardTransactionRepository extends JpaRepository<RewardTransactionEntity, UUID> {
    @Query("select coalesce(sum(t.points), 0) from RewardTransactionEntity t where t.userId = :userId")
    Integer sumPointsByUserId(@Param("userId") UUID userId);

    @Query("select coalesce(sum(t.points), 0) from RewardTransactionEntity t where t.userId = :userId and t.points > 0")
    Integer sumEarnedPointsByUserId(@Param("userId") UUID userId);

    @Query("select coalesce(sum(-t.points), 0) from RewardTransactionEntity t where t.userId = :userId and t.points < 0")
    Integer sumRedeemedPointsByUserId(@Param("userId") UUID userId);

    @Query("select coalesce(sum(t.points), 0) from RewardTransactionEntity t where t.userId = :userId and t.orderId = :orderId and t.type = :type")
    Integer sumPointsByUserIdAndOrderIdAndType(@Param("userId") UUID userId, @Param("orderId") UUID orderId, @Param("type") String type);

    boolean existsByUserIdAndOrderIdAndType(UUID userId, UUID orderId, String type);
}
