package ca.umika.api.payment;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttemptEntity, UUID> {
    long countByOrderId(UUID orderId);
}
