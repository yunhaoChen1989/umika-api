package ca.umika.api.notification;

import ca.umika.api.order.OrderResponse;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class OrderNotificationService {

    private static final String NEW_ORDER_AUTO_ACCEPTED = "NEW_ORDER_AUTO_ACCEPTED";
    private static final String ORDER_ACCEPTANCE_REQUESTED = "ORDER_ACCEPTANCE_REQUESTED";
    private static final String ORDER_STATUS_UPDATED = "ORDER_STATUS_UPDATED";

    private final OrderNotificationWebSocketHandler webSocketHandler;

    public OrderNotificationService(OrderNotificationWebSocketHandler webSocketHandler) {
        this.webSocketHandler = webSocketHandler;
    }

    public void notifyPaidOrder(OrderResponse order, boolean autoAccepted) {
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
    }

    public void notifyStatusUpdated(OrderResponse order) {
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
    }
}
