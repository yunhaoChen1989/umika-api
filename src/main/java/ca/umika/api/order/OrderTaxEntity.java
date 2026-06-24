package ca.umika.api.order;

import jakarta.persistence.*;
import ca.umika.api.common.persistence.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "order_taxes")
public class OrderTaxEntity extends BaseEntity {

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
}
