package ca.umika.api.email;

import ca.umika.api.order.OrderResponse;
import java.math.BigDecimal;

public record OrderEmailEvent(
        Type type,
        OrderResponse order,
        BigDecimal refundAmount,
        boolean fullyRefunded,
        String refundReason
) {
    public enum Type {
        ACCEPTED,
        READY,
        CANCELLED,
        REFUNDED
    }

    public static OrderEmailEvent accepted(OrderResponse order) {
        return new OrderEmailEvent(Type.ACCEPTED, order, null, false, null);
    }

    public static OrderEmailEvent ready(OrderResponse order) {
        return new OrderEmailEvent(Type.READY, order, null, false, null);
    }

    public static OrderEmailEvent cancelled(OrderResponse order) {
        return new OrderEmailEvent(Type.CANCELLED, order, null, false, null);
    }

    public static OrderEmailEvent refunded(
            OrderResponse order,
            BigDecimal refundAmount,
            boolean fullyRefunded,
            String refundReason
    ) {
        return new OrderEmailEvent(Type.REFUNDED, order, refundAmount, fullyRefunded, refundReason);
    }
}
