package ca.umika.api.notification;

import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.auth.JwtUtil;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class OrderNotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationWebSocketHandler.class);
    private static final String LOCATION_ID_ATTRIBUTE = "locationId";
    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String ORDER_MANAGE_PERMISSION = "ORDER_MANAGE";

    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AccountRoleService accountRoleService;
    private final UserPermissionRepository userPermissionRepository;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public OrderNotificationWebSocketHandler(
            ObjectMapper objectMapper,
            JwtUtil jwtUtil,
            UserRepository userRepository,
            AccountRoleService accountRoleService,
            UserPermissionRepository userPermissionRepository
    ) {
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.accountRoleService = accountRoleService;
        this.userPermissionRepository = userPermissionRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String locationId = queryValue(session.getUri(), "locationId");
        UserEntity user = authenticate(session);
        if (user == null || !canReceiveOrderNotifications(user, locationId)) {
            log.warn("order notification websocket rejected sessionId={} locationId={}", session.getId(), locationId);
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        session.getAttributes().put(LOCATION_ID_ATTRIBUTE, locationId);
        session.getAttributes().put(USER_ID_ATTRIBUTE, user.getId().toString());
        sessions.put(session.getId(), session);
        log.info("order notification websocket connected sessionId={} userId={} locationId={}",
                session.getId(), user.getId(), session.getAttributes().get(LOCATION_ID_ATTRIBUTE));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("order notification websocket disconnected sessionId={} status={}", session.getId(), status);
    }

    public void broadcast(OrderNotificationPayload payload) {
        TextMessage message;
        try {
            message = new TextMessage(objectMapper.writeValueAsString(payload));
        } catch (IOException exception) {
            log.warn("unable to serialize order notification orderId={} message={}", payload.orderId(), exception.getMessage());
            return;
        }

        for (WebSocketSession session : sessions.values()) {
            if (!session.isOpen() || !matchesLocation(session, payload.locationId())) {
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException exception) {
                log.warn("unable to send order notification sessionId={} orderId={} message={}",
                        session.getId(), payload.orderId(), exception.getMessage());
            }
        }
    }

    private boolean matchesLocation(WebSocketSession session, UUID locationId) {
        String sessionLocationId = (String) session.getAttributes().get(LOCATION_ID_ATTRIBUTE);
        return sessionLocationId == null || sessionLocationId.isBlank() || locationId == null || sessionLocationId.equalsIgnoreCase(locationId.toString());
    }

    private UserEntity authenticate(WebSocketSession session) {
        String token = queryValue(session.getUri(), "token");
        if (token == null || token.isBlank() || !jwtUtil.isTokenValid(token)) {
            return null;
        }
        String email = jwtUtil.getSubjectFromToken(token);
        return userRepository.findByEmail(email).orElse(null);
    }

    private boolean canReceiveOrderNotifications(UserEntity user, String locationIdValue) {
        if (accountRoleService.resolveRoleNames(user.getId()).stream().anyMatch(this::isStaffRole)) {
            return true;
        }
        if (locationIdValue == null || locationIdValue.isBlank()) {
            return userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(
                    user.getId(), ORDER_MANAGE_PERMISSION);
        }
        try {
            UUID locationId = UUID.fromString(locationIdValue);
            return userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(
                    user.getId(), ORDER_MANAGE_PERMISSION)
                    || userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(
                            user.getId(), ORDER_MANAGE_PERMISSION, locationId)
                    || locationId.equals(user.getLocationId());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isStaffRole(String roleName) {
        return "ROLE_ADMIN".equalsIgnoreCase(roleName)
                || "ROLE_MANAGER".equalsIgnoreCase(roleName)
                || "ROLE_STAFF".equalsIgnoreCase(roleName)
                || "ROLE_KITCHEN".equalsIgnoreCase(roleName);
    }

    private String queryValue(URI uri, String key) {
        if (uri == null || uri.getRawQuery() == null) {
            return null;
        }
        for (String pair : uri.getRawQuery().split("&")) {
            int separator = pair.indexOf('=');
            String pairKey = separator >= 0 ? pair.substring(0, separator) : pair;
            if (!key.equals(URLDecoder.decode(pairKey, StandardCharsets.UTF_8))) {
                continue;
            }
            String value = separator >= 0 ? pair.substring(separator + 1) : "";
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }
        return null;
    }
}
