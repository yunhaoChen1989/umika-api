package ca.umika.api.notification;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import ca.umika.api.email.OrderEmailPublisher;
import ca.umika.api.order.OrderResponse;
import ca.umika.api.printing.PrinterService;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderNotificationServiceTest {

    private OrderNotificationWebSocketHandler webSocketHandler;
    private PrinterService printerService;
    private OrderEmailPublisher emailPublisher;
    private OrderNotificationService service;

    @BeforeEach
    void setUp() {
        webSocketHandler = mock(OrderNotificationWebSocketHandler.class);
        printerService = mock(PrinterService.class);
        emailPublisher = mock(OrderEmailPublisher.class);
        service = new OrderNotificationService(webSocketHandler, printerService, emailPublisher);
    }

    @Test
    void autoAcceptedPaidOrderQueuesAcceptedEmail() {
        OrderResponse order = mock(OrderResponse.class);

        service.notifyPaidOrder(order, true);

        verify(printerService).enqueue(order);
        verify(emailPublisher).accepted(order);
    }

    @Test
    void paidOrderAwaitingManagerDoesNotEmailUntilAccepted() {
        OrderResponse order = mock(OrderResponse.class);

        service.notifyPaidOrder(order, false);

        verify(emailPublisher, never()).accepted(order);
    }

    @Test
    void enteringPreparingQueuesAcceptedEmailOnlyOnce() {
        OrderResponse order = mock(OrderResponse.class);
        org.mockito.Mockito.when(order.status()).thenReturn("PREPARING");

        service.notifyStatusUpdated(order, "PAID");
        service.notifyStatusUpdated(order, "PREPARING");

        verify(emailPublisher).accepted(order);
    }

    @Test
    void enteringReadyQueuesReadyEmailOnlyOnce() {
        OrderResponse order = mock(OrderResponse.class);
        org.mockito.Mockito.when(order.status()).thenReturn("READY");

        service.notifyStatusUpdated(order, "PREPARING");
        service.notifyStatusUpdated(order, "READY");

        verify(emailPublisher).ready(order);
    }

    @Test
    void enteringCancelledQueuesCancellationEmailOnlyOnce() {
        OrderResponse order = mock(OrderResponse.class);
        org.mockito.Mockito.when(order.status()).thenReturn("CANCELLED");

        service.notifyStatusUpdated(order, "PREPARING");
        service.notifyStatusUpdated(order, "CANCELLED");

        verify(emailPublisher).cancelled(order);
    }

    @Test
    void successfulRefundAlwaysQueuesRefundEmail() {
        OrderResponse order = mock(OrderResponse.class);
        org.mockito.Mockito.when(order.status()).thenReturn("PARTIALLY_REFUNDED");
        BigDecimal amount = new BigDecimal("12.50");

        service.notifyRefunded(order, "PAID", amount, false, "Customer request");

        verify(emailPublisher).refunded(order, amount, false, "Customer request");
    }
}
