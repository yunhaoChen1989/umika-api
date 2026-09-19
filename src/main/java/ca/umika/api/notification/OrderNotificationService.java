package ca.umika.api.notification;

import ca.umika.api.order.OrderResponse;
import ca.umika.api.email.OrderEmailPublisher;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class OrderNotificationService {

    private static final String NEW_ORDER_AUTO_ACCEPTED = "NEW_ORDER_AUTO_ACCEPTED";
    private static final String ORDER_ACCEPTANCE_REQUESTED = "ORDER_ACCEPTANCE_REQUESTED";
    private static final String ORDER_STATUS_UPDATED = "ORDER_STATUS_UPDATED";

    private final OrderNotificationWebSocketHandler webSocketHandler;
    private final ca.umika.api.printing.PrinterService printerService;
    private final OrderEmailPublisher orderEmailPublisher;

    public OrderNotificationService(
            OrderNotificationWebSocketHandler webSocketHandler,
            ca.umika.api.printing.PrinterService printerService,
            OrderEmailPublisher orderEmailPublisher
    ) {
        this.webSocketHandler = webSocketHandler;
        this.printerService = printerService;
        this.orderEmailPublisher = orderEmailPublisher;
    }

    public void notifyPaidOrder(OrderResponse order, boolean autoAccepted) {
        printerService.enqueue(order);
        OrderNotificationPayload payload = new OrderNotificationPayload(
                autoAccepted ? NEW_ORDER_AUTO_ACCEPTED : ORDER_ACCEPTANCE_REQUESTED,
                order.id(),
                order.locationId(),
                order.orderNumber(),
                order.status(),
                autoAccepted,
                !autoAccepted,
                order,
                LocalDateTime.now()
        );
        webSocketHandler.broadcast(payload);
        if (autoAccepted) {
            orderEmailPublisher.accepted(order);
        }
    }

    public void notifyStatusUpdated(OrderResponse order, String previousStatus) {
        OrderNotificationPayload payload = new OrderNotificationPayload(
                ORDER_STATUS_UPDATED,
                order.id(),
                order.locationId(),
                order.orderNumber(),
                order.status(),
                false,
                false,
                order,
                LocalDateTime.now()
        );
        webSocketHandler.broadcast(payload);
        if ("PREPARING".equalsIgnoreCase(order.status()) && !"PREPARING".equalsIgnoreCase(previousStatus)) {
            orderEmailPublisher.accepted(order);
        } else if ("READY".equalsIgnoreCase(order.status()) && !"READY".equalsIgnoreCase(previousStatus)) {
            orderEmailPublisher.ready(order);
        } else if ("CANCELLED".equalsIgnoreCase(order.status()) && !"CANCELLED".equalsIgnoreCase(previousStatus)) {
            orderEmailPublisher.cancelled(order);
        }
    }

    public void notifyRefunded(
            OrderResponse order,
            String previousStatus,
            BigDecimal amount,
            boolean fullyRefunded,
            String reason
    ) {
        notifyStatusUpdated(order, previousStatus);
        orderEmailPublisher.refunded(order, amount, fullyRefunded, reason);
    }
}
