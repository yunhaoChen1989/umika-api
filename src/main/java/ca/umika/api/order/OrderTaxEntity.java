package ca.umika.api.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "order_taxes")
public class OrderTaxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "tax_rule_id")
    private UUID taxRuleId;

    @Column(name = "tax_name")
    private String taxName;

    @Column(name = "tax_rate")
    private BigDecimal taxRate;

    @Column(name = "taxable_amount")
    private BigDecimal taxableAmount;

    @Column(name = "tax_amount")
    private BigDecimal taxAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public UUID getTaxRuleId() {
        return taxRuleId;
    }

    public void setTaxRuleId(UUID taxRuleId) {
        this.taxRuleId = taxRuleId;
    }

    public String getTaxName() {
        return taxName;
    }

    public void setTaxName(String taxName) {
        this.taxName = taxName;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxableAmount() {
        return taxableAmount;
    }

    public void setTaxableAmount(BigDecimal taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
