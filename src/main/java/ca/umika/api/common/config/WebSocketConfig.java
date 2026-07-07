package ca.umika.api.common.config;

import ca.umika.api.notification.OrderNotificationWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final OrderNotificationWebSocketHandler orderNotificationWebSocketHandler;

    public WebSocketConfig(OrderNotificationWebSocketHandler orderNotificationWebSocketHandler) {
        this.orderNotificationWebSocketHandler = orderNotificationWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderNotificationWebSocketHandler, "/api/v1/manager/order-notifications/ws", "/api/manager/order-notifications/ws")
                .setAllowedOriginPatterns("*");
    }
}
