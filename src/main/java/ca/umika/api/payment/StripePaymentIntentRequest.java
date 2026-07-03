package ca.umika.api.payment;

import java.util.UUID;

public record StripePaymentIntentRequest(
        UUID orderId
) {
}
