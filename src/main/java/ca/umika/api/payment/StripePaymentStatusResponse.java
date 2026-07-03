package ca.umika.api.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record StripePaymentStatusResponse(
        UUID orderId,
        UUID paymentTransactionId,
        String paymentIntentId,
        String status,
        BigDecimal amount,
        String currency,
        String orderStatus
) {
}
