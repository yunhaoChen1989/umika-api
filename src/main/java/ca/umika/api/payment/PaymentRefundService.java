package ca.umika.api.payment;

import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.order.OrderEntity;
import ca.umika.api.order.OrderRepository;
import ca.umika.api.order.OrderService;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PaymentRefundService {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundService.class);

    private static final String PROVIDER = "STRIPE";
    private static final String ORDER_MANAGE_PERMISSION = "ORDER_MANAGE";
    private static final Set<String> REFUNDABLE_PAYMENT_STATUSES = Set.of("PAID", "PARTIALLY_REFUNDED");

    private final PaymentRefundRepository refundRepository;
    private final PaymentRefundMapper mapper;
    private final PaymentTransactionRepository transactionRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final AccountRoleService accountRoleService;
    private final StripeProperties stripeProperties;

    public PaymentRefundService(
            PaymentRefundRepository refundRepository,
            PaymentRefundMapper mapper,
            PaymentTransactionRepository transactionRepository,
            OrderRepository orderRepository,
            OrderService orderService,
            UserRepository userRepository,
            UserPermissionRepository userPermissionRepository,
            AccountRoleService accountRoleService,
            StripeProperties stripeProperties
    ) {
        this.refundRepository = refundRepository;
        this.mapper = mapper;
        this.transactionRepository = transactionRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.accountRoleService = accountRoleService;
        this.stripeProperties = stripeProperties;
    }

    @Transactional(readOnly = true)
    public List<PaymentRefundDto> findByOrder(Authentication authentication, UUID orderId) {
        OrderEntity order = findOrder(orderId);
        UserEntity user = resolveUser(authentication);
        assertCanManage(user, order.getLocationId());
        return refundRepository.findByOrderIdOrderByCreatedAtDesc(orderId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public PaymentRefundDto refund(Authentication authentication, UUID orderId, PaymentRefundRequest request) {
        OrderEntity order = findOrder(orderId);
        UserEntity actor = resolveUser(authentication);
        assertCanManage(actor, order.getLocationId());
        ensureStripeConfigured();

        String idempotencyKey = normalizeIdempotencyKey(request == null ? null : request.idempotencyKey());
        RefundCreateParams.Reason stripeReason = resolveStripeReason(request == null ? null : request.stripeReason());
        PaymentRefundEntity existing = refundRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            if (!existing.getOrderId().equals(orderId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Refund idempotency key was already used for another order");
            }
            if (existing.getProviderRefundId() != null || !"PENDING".equalsIgnoreCase(existing.getStatus())) {
                return mapper.toDto(existing);
            }
        }

        PaymentTransactionEntity transaction = transactionRepository
                .findFirstByOrderIdAndProviderOrderByCreatedAtDesc(orderId, PROVIDER)
                .orElseThrow(() -> new ResourceNotFoundException("Stripe payment transaction not found for order: " + orderId));
        if (!REFUNDABLE_PAYMENT_STATUSES.contains(normalize(transaction.getStatus()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only a paid Stripe transaction can be refunded");
        }
        if (transaction.getProviderIntentId() == null || transaction.getProviderIntentId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe payment intent is missing for this order");
        }

        BigDecimal paidAmount = money(transaction.getAmount());
        BigDecimal alreadyRefunded = money(refundRepository.sumSuccessfulAmountByPaymentTransactionId(transaction.getId()));
        BigDecimal refundableAmount = paidAmount.subtract(alreadyRefunded).setScale(2, RoundingMode.HALF_UP);
        if (refundableAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order has already been fully refunded");
        }
        BigDecimal requestedAmount = normalizeAmount(
                existing == null ? (request == null ? null : request.amount()) : existing.getAmount(),
                refundableAmount
        );

        PaymentRefundEntity refundRecord = existing == null ? new PaymentRefundEntity() : existing;
        if (existing == null) {
            refundRecord.setPaymentTransactionId(transaction.getId());
            refundRecord.setOrderId(order.getId());
            refundRecord.setUserId(order.getUserId());
            refundRecord.setRequestedBy(actor.getId());
            refundRecord.setAmount(requestedAmount);
            refundRecord.setReason(trimToNull(request == null ? null : request.reason()));
            refundRecord.setIdempotencyKey(idempotencyKey);
            refundRecord.setStatus("PENDING");
            refundRecord = refundRepository.saveAndFlush(refundRecord);
        }

        try {
            Refund stripeRefund = Refund.create(
                    RefundCreateParams.builder()
                            .setPaymentIntent(transaction.getProviderIntentId())
                            .setAmount(toMinorUnits(requestedAmount))
                            .setReason(stripeReason)
                            .putMetadata("orderId", order.getId().toString())
                            .putMetadata("orderNumber", order.getOrderNumber())
                            .putMetadata("refundRecordId", refundRecord.getId().toString())
                            .putMetadata("requestedBy", actor.getId().toString())
                            .build(),
                    requestOptions(idempotencyKey)
            );
            refundRecord.setProviderRefundId(stripeRefund.getId());
            refundRecord.setStatus(mapStripeRefundStatus(stripeRefund.getStatus()));
            refundRecord.setFailureReason(stripeRefund.getFailureReason());
            refundRecord = refundRepository.save(refundRecord);

            if ("SUCCESS".equals(refundRecord.getStatus())) {
                applySuccessfulRefund(refundRecord, transaction);
            }

            log.info("stripe refund created orderId={} orderNumber={} refundId={} amount={} status={} requestedBy={}",
                    order.getId(), order.getOrderNumber(), stripeRefund.getId(), requestedAmount, refundRecord.getStatus(), actor.getId());
            return mapper.toDto(refundRecord);
        } catch (StripeException exception) {
            refundRecord.setStatus("FAILED");
            refundRecord.setFailureReason(exception.getMessage());
            refundRepository.save(refundRecord);
            log.warn("stripe refund failed orderId={} transactionId={} amount={} requestedBy={} message={}",
                    order.getId(), transaction.getId(), requestedAmount, actor.getId(), exception.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to refund Stripe payment: " + exception.getMessage());
        }
    }

    @Transactional
    public PaymentRefundDto applyStripeRefundWebhook(Refund stripeRefund) {
        if (stripeRefund == null || trimToNull(stripeRefund.getId()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe refund payload is missing an id");
        }
        PaymentRefundEntity refundRecord = refundRepository.findByProviderRefundId(stripeRefund.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment refund not found for Stripe refund: " + stripeRefund.getId()));
        String previousStatus = refundRecord.getStatus();
        refundRecord.setStatus(mapStripeRefundStatus(stripeRefund.getStatus()));
        refundRecord.setFailureReason(stripeRefund.getFailureReason());
        refundRecord = refundRepository.save(refundRecord);

        if ("SUCCESS".equals(refundRecord.getStatus()) && !"SUCCESS".equalsIgnoreCase(previousStatus)) {
            UUID paymentTransactionId = refundRecord.getPaymentTransactionId();
            PaymentTransactionEntity transaction = transactionRepository.findById(paymentTransactionId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Payment transaction not found: " + paymentTransactionId));
            applySuccessfulRefund(refundRecord, transaction);
        }
        log.info("stripe refund webhook applied orderId={} refundId={} previousStatus={} status={}",
                refundRecord.getOrderId(), stripeRefund.getId(), previousStatus, refundRecord.getStatus());
        return mapper.toDto(refundRecord);
    }

    private void applySuccessfulRefund(PaymentRefundEntity refundRecord, PaymentTransactionEntity transaction) {
        BigDecimal paidAmount = money(transaction.getAmount());
        BigDecimal cumulativeRefunded = money(
                refundRepository.sumSuccessfulAmountByPaymentTransactionId(transaction.getId()));
        boolean fullyRefunded = cumulativeRefunded.compareTo(paidAmount) >= 0;
        transaction.setStatus(fullyRefunded ? "REFUNDED" : "PARTIALLY_REFUNDED");
        transactionRepository.save(transaction);
        orderService.markRefundedFromPayment(
                refundRecord.getOrderId(),
                refundRecord.getRequestedBy(),
                fullyRefunded,
                refundRecord.getReason(),
                refundRecord.getAmount()
        );
    }

    private OrderEntity findOrder(UUID orderId) {
        if (orderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required");
        }
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
    }

    private UserEntity resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    private void assertCanManage(UserEntity user, UUID locationId) {
        if (accountRoleService.resolveRoleNames(user.getId()).contains("ROLE_ADMIN")) {
            return;
        }
        boolean global = userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(
                user.getId(), ORDER_MANAGE_PERMISSION);
        boolean locationPermission = locationId != null
                && userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(
                        user.getId(), ORDER_MANAGE_PERMISSION, locationId);
        List<String> roles = accountRoleService.resolveRoleNames(user.getId());
        boolean assignedStoreRole = locationId != null
                && (roles.contains("ROLE_MANAGER") || roles.contains("ROLE_STAFF"))
                && locationId.equals(user.getLocationId());
        if (!global && !locationPermission && !assignedStoreRole) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing order permission for refund");
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount, BigDecimal refundableAmount) {
        BigDecimal normalized = amount == null ? refundableAmount : money(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund amount must be greater than zero");
        }
        if (normalized.compareTo(refundableAmount) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund amount exceeds the remaining refundable amount of " + refundableAmount);
        }
        return normalized;
    }

    private String normalizeIdempotencyKey(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idempotencyKey is required");
        }
        if (normalized.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idempotencyKey cannot exceed 100 characters");
        }
        return normalized;
    }

    private RefundCreateParams.Reason resolveStripeReason(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "DUPLICATE" -> RefundCreateParams.Reason.DUPLICATE;
            case "FRAUDULENT" -> RefundCreateParams.Reason.FRAUDULENT;
            case "", "REQUESTED_BY_CUSTOMER" -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "stripeReason must be REQUESTED_BY_CUSTOMER, DUPLICATE, or FRAUDULENT"
            );
        };
    }

    private String mapStripeRefundStatus(String status) {
        return switch (normalize(status)) {
            case "SUCCEEDED" -> "SUCCESS";
            case "PENDING", "REQUIRES_ACTION" -> "PENDING";
            default -> "FAILED";
        };
    }

    private RequestOptions requestOptions(String idempotencyKey) {
        return RequestOptions.builder()
                .setApiKey(trimToNull(stripeProperties.secretKey()))
                .setIdempotencyKey("refund-" + idempotencyKey)
                .build();
    }

    private void ensureStripeConfigured() {
        String secretKey = trimToNull(stripeProperties.secretKey());
        if (secretKey == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe secret key is not configured");
        }
        if (secretKey.startsWith("pk_")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stripe refunds require a server secret or restricted key");
        }
    }

    private long toMinorUnits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
