package ca.umika.api.payment;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefundEntity, UUID> {
}
