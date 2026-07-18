package ca.umika.api.payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefundEntity, UUID> {
    List<PaymentRefundEntity> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    Optional<PaymentRefundEntity> findByIdempotencyKey(String idempotencyKey);

    Optional<PaymentRefundEntity> findByProviderRefundId(String providerRefundId);

    @Query("""
            select coalesce(sum(r.amount), 0)
            from PaymentRefundEntity r
            where r.paymentTransactionId = :paymentTransactionId
              and r.status = 'SUCCESS'
            """)
    BigDecimal sumSuccessfulAmountByPaymentTransactionId(@Param("paymentTransactionId") UUID paymentTransactionId);
}
