package ca.umika.api.payment;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, UUID> {
    Optional<PaymentTransactionEntity> findFirstByOrderIdAndProviderOrderByCreatedAtDesc(UUID orderId, String provider);
    Optional<PaymentTransactionEntity> findFirstByProviderIntentIdOrderByCreatedAtDesc(String providerIntentId);
}
