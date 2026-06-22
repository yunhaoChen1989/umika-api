package ca.umika.api.payment;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, UUID> {
}
