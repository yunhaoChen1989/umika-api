package ca.umika.api.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderRedemptionPreviewResponse(
        UUID cartId,
        UUID userId,
        UUID locationId,
        Integer availablePoints,
        Integer requestedPoints,
        Integer appliedPoints,
        Integer maxRedeemablePoints,
        BigDecimal pointValue,
        BigDecimal redemptionAmount,
        BigDecimal subtotal,
        BigDecimal taxableAmount,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal tipAmount,
        BigDecimal finalTotal
) {
}
