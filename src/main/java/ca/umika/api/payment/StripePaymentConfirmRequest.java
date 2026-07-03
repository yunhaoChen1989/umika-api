package ca.umika.api.payment;

import java.util.UUID;

public record StripePaymentConfirmRequest(
        UUID orderId,
        String paymentIntentId
) {
}
