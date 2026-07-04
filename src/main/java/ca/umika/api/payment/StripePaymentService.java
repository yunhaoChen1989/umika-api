package ca.umika.api.payment;

import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.order.OrderEntity;
import ca.umika.api.order.OrderRepository;
import ca.umika.api.order.OrderService;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class StripePaymentService {

    private static final String PROVIDER = "STRIPE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_REQUIRES_ACTION = "REQUIRES_ACTION";

    private final StripeProperties properties;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final PaymentWebhookLogRepository webhookLogRepository;
    private final ObjectMapper objectMapper;

    public StripePaymentService(
            StripeProperties properties,
            OrderRepository orderRepository,
            OrderService orderService,
            UserRepository userRepository,
            PaymentTransactionRepository transactionRepository,
            PaymentAttemptRepository attemptRepository,
            PaymentWebhookLogRepository webhookLogRepository,
            ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.attemptRepository = attemptRepository;
        this.webhookLogRepository = webhookLogRepository;
        this.objectMapper = objectMapper;
    }

    public StripePaymentIntentResponse createPaymentIntent(Authentication authentication, StripePaymentIntentRequest request) {
        UserEntity user = resolveUser(authentication);
        OrderEntity order = resolveUserOrder(request == null ? null : request.orderId(), user.getId());
        ensureStripeConfigured();

        PaymentTransactionEntity existing = transactionRepository
                .findFirstByOrderIdAndProviderOrderByCreatedAtDesc(order.getId(), PROVIDER)
                .orElse(null);
        if (existing != null && existing.getProviderIntentId() != null && !STATUS_FAILED.equalsIgnoreCase(existing.getStatus())) {
            PaymentIntent paymentIntent = retrievePaymentIntent(existing.getProviderIntentId());
            updateTransactionFromIntent(existing, paymentIntent, null);
            if ("succeeded".equalsIgnoreCase(paymentIntent.getStatus())) {
                orderService.markPaidFromPayment(order.getId(), user.getId(), "Stripe payment already succeeded");
                return toIntentResponse(existing, paymentIntent);
            }
            if (canReusePaymentIntent(paymentIntent)) {
                return toIntentResponse(existing, paymentIntent);
            }
        }

        PaymentAttemptEntity attempt = createAttempt(order, "INITIATED", Map.of(
                "orderId", order.getId().toString(),
                "amount", order.getFinalTotal(),
                "currency", properties.resolvedCurrency()
        ));
        String idempotencyKey = existing == null
                ? "order-" + order.getId() + "-payment-intent"
                : "order-" + order.getId() + "-payment-intent-" + attempt.getAttemptNumber();

        try {
            PaymentIntent paymentIntent = PaymentIntent.create(
                    PaymentIntentCreateParams.builder()
                            .setAmount(toMinorUnits(order.getFinalTotal()))
                            .setCurrency(properties.resolvedCurrency())
                            .setDescription("Umika Sushi order " + order.getOrderNumber())
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                            .setEnabled(true)
                                            .build()
                            )
                            .putMetadata("orderId", order.getId().toString())
                            .putMetadata("orderNumber", order.getOrderNumber())
                            .putMetadata("userId", user.getId().toString())
                            .build(),
                    requestOptions(idempotencyKey)
            );

            PaymentTransactionEntity transaction = transactionRepository
                    .findFirstByProviderIntentIdOrderByCreatedAtDesc(paymentIntent.getId())
                    .orElseGet(PaymentTransactionEntity::new);
            transaction.setOrderId(order.getId());
            transaction.setUserId(user.getId());
            transaction.setProvider(PROVIDER);
            transaction.setProviderIntentId(paymentIntent.getId());
            transaction.setProviderPaymentId(paymentIntent.getLatestCharge());
            transaction.setStatus(mapPaymentIntentStatus(paymentIntent.getStatus()));
            transaction.setAmount(order.getFinalTotal());
            transaction.setCurrency(properties.resolvedCurrency().toUpperCase());
            transactionRepository.save(transaction);

            attempt.setStatus("SUCCESS");
            attempt.setResponsePayload(Map.of(
                    "paymentIntentId", paymentIntent.getId(),
                    "status", paymentIntent.getStatus()
            ));
            attemptRepository.save(attempt);
            if ("succeeded".equalsIgnoreCase(paymentIntent.getStatus())) {
                orderService.markPaidFromPayment(order.getId(), user.getId(), "Stripe payment succeeded");
            }
            return toIntentResponse(transaction, paymentIntent);
        } catch (StripeException exception) {
            attempt.setStatus("FAILED");
            attempt.setErrorMessage(exception.getMessage());
            attemptRepository.save(attempt);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to create Stripe payment intent: " + exception.getMessage());
        }
    }

    public StripePaymentStatusResponse confirm(Authentication authentication, StripePaymentConfirmRequest request) {
        UserEntity user = resolveUser(authentication);
        UUID orderId = request == null ? null : request.orderId();
        String paymentIntentId = request == null ? null : trimToNull(request.paymentIntentId());
        PaymentTransactionEntity transaction = resolveTransaction(orderId, paymentIntentId);
        if (!transaction.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Payment access denied");
        }

        PaymentIntent paymentIntent = retrievePaymentIntent(transaction.getProviderIntentId());
        return applyPaymentIntent(paymentIntent, user.getId());
    }

    public StripePaymentStatusResponse handleWebhook(String payload, String signature) {
        ensureStripeConfigured();
        if (properties.webhookSecret() == null || properties.webhookSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe webhook secret is not configured");
        }
        try {
            Event event = Webhook.constructEvent(payload, signature, properties.webhookSecret());
            PaymentWebhookLogEntity log = new PaymentWebhookLogEntity();
            log.setProvider(PROVIDER);
            log.setEventType(event.getType());
            log.setProviderEventId(event.getId());
            log.setPayload(parsePayload(payload));
            log.setProcessed(false);
            webhookLogRepository.save(log);

            StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
            if (object instanceof PaymentIntent paymentIntent) {
                StripePaymentStatusResponse response = applyPaymentIntent(paymentIntent, null);
                log.setProcessed(true);
                webhookLogRepository.save(log);
                return response;
            }

            log.setProcessed(true);
            webhookLogRepository.save(log);
            return new StripePaymentStatusResponse(null, null, null, "IGNORED", null, properties.resolvedCurrency().toUpperCase(), null);
        } catch (SignatureVerificationException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe webhook signature");
        } catch (StripeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to process Stripe webhook: " + exception.getMessage());
        }
    }

    private StripePaymentStatusResponse applyPaymentIntent(PaymentIntent paymentIntent, UUID changedBy) {
        PaymentTransactionEntity transaction = transactionRepository.findFirstByProviderIntentIdOrderByCreatedAtDesc(paymentIntent.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found for Stripe intent: " + paymentIntent.getId()));
        updateTransactionFromIntent(transaction, paymentIntent, null);

        String orderStatus = null;
        if ("succeeded".equalsIgnoreCase(paymentIntent.getStatus())) {
            orderStatus = orderService
                    .markPaidFromPayment(transaction.getOrderId(), changedBy == null ? transaction.getUserId() : changedBy, "Stripe payment succeeded")
                    .status();
        } else {
            orderStatus = orderRepository.findById(transaction.getOrderId())
                    .map(OrderEntity::getStatus)
                    .orElse(null);
        }

        return new StripePaymentStatusResponse(
                transaction.getOrderId(),
                transaction.getId(),
                transaction.getProviderIntentId(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getCurrency(),
                orderStatus
        );
    }

    private void updateTransactionFromIntent(PaymentTransactionEntity transaction, PaymentIntent paymentIntent, String failureReason) {
        transaction.setProviderIntentId(paymentIntent.getId());
        transaction.setProviderPaymentId(paymentIntent.getLatestCharge());
        transaction.setStatus(mapPaymentIntentStatus(paymentIntent.getStatus()));
        transaction.setFailureReason(failureReason == null ? resolveFailureReason(paymentIntent) : failureReason);
        transactionRepository.save(transaction);
    }

    private PaymentTransactionEntity resolveTransaction(UUID orderId, String paymentIntentId) {
        if (paymentIntentId != null) {
            return transactionRepository.findFirstByProviderIntentIdOrderByCreatedAtDesc(paymentIntentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found for Stripe intent: " + paymentIntentId));
        }
        if (orderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId or paymentIntentId is required");
        }
        return transactionRepository.findFirstByOrderIdAndProviderOrderByCreatedAtDesc(orderId, PROVIDER)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found for order: " + orderId));
    }

    private PaymentIntent retrievePaymentIntent(String paymentIntentId) {
        try {
            return PaymentIntent.retrieve(paymentIntentId, requestOptions(null));
        } catch (StripeException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to retrieve Stripe payment intent: " + exception.getMessage());
        }
    }

    private PaymentAttemptEntity createAttempt(OrderEntity order, String status, Map<String, Object> requestPayload) {
        PaymentAttemptEntity attempt = new PaymentAttemptEntity();
        attempt.setOrderId(order.getId());
        attempt.setUserId(order.getUserId());
        attempt.setAttemptNumber((int) attemptRepository.countByOrderId(order.getId()) + 1);
        attempt.setStatus(status);
        attempt.setRequestPayload(requestPayload);
        return attemptRepository.save(attempt);
    }

    private StripePaymentIntentResponse toIntentResponse(PaymentTransactionEntity transaction, PaymentIntent paymentIntent) {
        String status = mapPaymentIntentStatus(paymentIntent.getStatus());
        String clientSecret = canReusePaymentIntent(paymentIntent) ? paymentIntent.getClientSecret() : null;
        return new StripePaymentIntentResponse(
                transaction.getOrderId(),
                transaction.getId(),
                paymentIntent.getId(),
                clientSecret,
                status,
                transaction.getAmount(),
                transaction.getCurrency(),
                properties.publishableKey()
        );
    }

    private OrderEntity resolveUserOrder(UUID orderId, UUID userId) {
        if (orderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required");
        }
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (!userId.equals(order.getUserId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Order access denied");
        }
        if (order.getFinalTotal() == null || order.getFinalTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order total must be greater than zero");
        }
        return order;
    }

    private UserEntity resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    private RequestOptions requestOptions(String idempotencyKey) {
        RequestOptions.RequestOptionsBuilder builder = RequestOptions.builder().setApiKey(trimToNull(properties.secretKey()));
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            builder.setIdempotencyKey(idempotencyKey);
        }
        return builder.build();
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String mapPaymentIntentStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return STATUS_PENDING;
        }
        return switch (stripeStatus) {
            case "succeeded" -> STATUS_PAID;
            case "requires_action", "requires_confirmation", "requires_payment_method" -> STATUS_REQUIRES_ACTION;
            case "canceled" -> STATUS_CANCELLED;
            case "processing", "requires_capture" -> STATUS_PENDING;
            default -> STATUS_FAILED;
        };
    }

    private boolean canReusePaymentIntent(PaymentIntent paymentIntent) {
        if (paymentIntent == null || paymentIntent.getStatus() == null) {
            return false;
        }
        return switch (paymentIntent.getStatus()) {
            case "requires_payment_method", "requires_confirmation", "requires_action", "processing", "requires_capture" -> true;
            default -> false;
        };
    }

    private String resolveFailureReason(PaymentIntent paymentIntent) {
        if (paymentIntent.getLastPaymentError() == null) {
            return null;
        }
        return paymentIntent.getLastPaymentError().getMessage();
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception ignored) {
            return Map.of("raw", payload);
        }
    }

    private void ensureStripeConfigured() {
        String secretKey = trimToNull(properties.secretKey());
        String publishableKey = trimToNull(properties.publishableKey());

        if (secretKey == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe secret key is not configured");
        }
        if (secretKey.startsWith("pk_")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe secret key must use an sk_ or restricted rk_ server key, not a pk_ publishable key");
        }
        if (publishableKey == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe publishable key is not configured");
        }
        if (!publishableKey.startsWith("pk_")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe publishable key must use a pk_ key");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
