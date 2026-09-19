package ca.umika.api.email;

import ca.umika.api.order.OrderResponse;
import java.math.BigDecimal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class OrderEmailPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public OrderEmailPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void accepted(OrderResponse order) {
        eventPublisher.publishEvent(OrderEmailEvent.accepted(order));
    }

    public void ready(OrderResponse order) {
        eventPublisher.publishEvent(OrderEmailEvent.ready(order));
    }

    public void cancelled(OrderResponse order) {
        eventPublisher.publishEvent(OrderEmailEvent.cancelled(order));
    }

    public void refunded(OrderResponse order, BigDecimal amount, boolean fullyRefunded, String reason) {
        eventPublisher.publishEvent(OrderEmailEvent.refunded(order, amount, fullyRefunded, reason));
    }
}
