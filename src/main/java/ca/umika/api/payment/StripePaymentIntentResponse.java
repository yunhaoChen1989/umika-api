package ca.umika.api.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record StripePaymentIntentResponse(
        UUID orderId,
        UUID paymentTransactionId,
        String paymentIntentId,
        String clientSecret,
        String status,
        BigDecimal amount,
        String currency,
        String publishableKey
) {
}
