package ca.umika.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        UUID locationId,
        UUID addressId,
        String orderNumber,
        String orderType,
        String status,
        BigDecimal subtotal,
        BigDecimal totalDiscount,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal tipAmount,
        BigDecimal finalTotal,
        LocalDateTime requestedPickupTime,
        String customerNote,
        String internalNote,
        Integer pointsRedeemed,
        BigDecimal rewardDiscountAmount,
        Integer pointsEarned,
        List<OrderItemResponse> items,
        List<OrderTaxResponse> taxes,
        List<OrderDiscountResponse> discounts,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record OrderItemResponse(
            UUID id,
            UUID menuItemId,
            String itemName,
            Integer quantity,
            BigDecimal unitPrice,
            String imageUrl,
            BigDecimal totalPrice,
            Map<String, Object> optionSnapshot
    ) {
    }

    public record OrderTaxResponse(
            UUID id,
            String taxName,
            BigDecimal taxRate,
            BigDecimal taxableAmount,
            BigDecimal taxAmount
    ) {
    }

    public record OrderDiscountResponse(
            UUID id,
            String discountType,
            BigDecimal amount,
            Map<String, Object> metadata
    ) {
    }
}
