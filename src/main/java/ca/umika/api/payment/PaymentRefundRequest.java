package ca.umika.api.payment;

import java.math.BigDecimal;

public record PaymentRefundRequest(
        BigDecimal amount,
        String reason,
        String stripeReason,
        String idempotencyKey
) {
}
