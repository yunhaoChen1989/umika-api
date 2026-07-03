package ca.umika.api.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        String publishableKey,
        String secretKey,
        String webhookSecret,
        String currency
) {
    public String resolvedCurrency() {
        return currency == null || currency.isBlank() ? "cad" : currency.trim().toLowerCase();
    }
}
