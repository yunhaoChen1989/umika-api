package ca.umika.api.payment;

import jakarta.persistence.*;
import ca.umika.api.common.persistence.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payment_refunds")
public class PaymentRefundEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payment_transaction_id", nullable = false)
    private UUID paymentTransactionId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "reason")
    private String reason;

    @Column(name = "provider_refund_id")
    private String providerRefundId;

    @Column(name = "status", nullable = false)
    private String status;
public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public void setPaymentTransactionId(UUID paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getProviderRefundId() {
        return providerRefundId;
    }

    public void setProviderRefundId(String providerRefundId) {
        this.providerRefundId = providerRefundId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
