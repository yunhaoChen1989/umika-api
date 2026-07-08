package ca.umika.api.order;

import ca.umika.api.admin.SystemSettingRepository;
import ca.umika.api.admin.UserPermissionEntity;
import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.cart.CartEntity;
import ca.umika.api.cart.CartItemEntity;
import ca.umika.api.cart.CartItemRepository;
import ca.umika.api.cart.CartRepository;
import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.notification.OrderNotificationService;
import ca.umika.api.referral.ReferralEntity;
import ca.umika.api.referral.ReferralRepository;
import ca.umika.api.reward.RewardRedemptionEntity;
import ca.umika.api.reward.RewardRedemptionRepository;
import ca.umika.api.reward.RewardTransactionEntity;
import ca.umika.api.reward.RewardTransactionRepository;
import ca.umika.api.reward.RewardWalletEntity;
import ca.umika.api.reward.RewardWalletRepository;
import ca.umika.api.store.LocationRepository;
import ca.umika.api.store.LocationSettingRepository;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserProfileEntity;
import ca.umika.api.user.UserProfileRepository;
import ca.umika.api.user.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final String ACTIVE_CART = "ACTIVE";
    private static final String CHECKED_OUT_CART = "CHECKED_OUT";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_PREPARING = "PREPARING";
    private static final String ORDER_MANAGE_PERMISSION = "ORDER_MANAGE";

    private static final String POINTS_PER_DOLLAR = "POINTS_PER_DOLLAR";
    private static final String POINT_VALUE_CENTS = "POINT_VALUE_CENTS";
    private static final String MAX_REDEMPTION_PERCENT = "MAX_REDEMPTION_PERCENT";
    private static final String DEFAULT_TAX_RATE = "DEFAULT_TAX_RATE";
    private static final String REFERRAL_FIRST_ORDER_POINTS = "REFERRAL_FIRST_ORDER_POINTS";
    private static final String MIN_REFERRAL_ORDER_AMOUNT = "MIN_REFERRAL_ORDER_AMOUNT";
    private static final String MIN_PICKUP_TIME_MINUTES = "MIN_PICKUP_TIME_MINUTES";
    private static final String AUTO_ACCEPT_ORDERS = "AUTO_ACCEPT_ORDERS";

    private final OrderRepository repository;
    private final OrderItemRepository orderItemRepository;
    private final OrderDiscountRepository orderDiscountRepository;
    private final OrderTaxRepository orderTaxRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final LocationRepository locationRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final LocationSettingRepository locationSettingRepository;
    private final RewardTransactionRepository rewardTransactionRepository;
    private final RewardRedemptionRepository rewardRedemptionRepository;
    private final RewardWalletRepository rewardWalletRepository;
    private final ReferralRepository referralRepository;
    private final AccountRoleService accountRoleService;
    private final UserPermissionRepository userPermissionRepository;
    private final OrderNotificationService orderNotificationService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OrderService(
            OrderRepository repository,
            OrderItemRepository orderItemRepository,
            OrderDiscountRepository orderDiscountRepository,
            OrderTaxRepository orderTaxRepository,
            OrderStatusHistoryRepository statusHistoryRepository,
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            LocationRepository locationRepository,
            SystemSettingRepository systemSettingRepository,
            LocationSettingRepository locationSettingRepository,
            RewardTransactionRepository rewardTransactionRepository,
            RewardRedemptionRepository rewardRedemptionRepository,
            RewardWalletRepository rewardWalletRepository,
            ReferralRepository referralRepository,
            AccountRoleService accountRoleService,
            UserPermissionRepository userPermissionRepository,
            OrderNotificationService orderNotificationService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.orderItemRepository = orderItemRepository;
        this.orderDiscountRepository = orderDiscountRepository;
        this.orderTaxRepository = orderTaxRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.locationRepository = locationRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.locationSettingRepository = locationSettingRepository;
        this.rewardTransactionRepository = rewardTransactionRepository;
        this.rewardRedemptionRepository = rewardRedemptionRepository;
        this.rewardWalletRepository = rewardWalletRepository;
        this.referralRepository = referralRepository;
        this.accountRoleService = accountRoleService;
        this.userPermissionRepository = userPermissionRepository;
        this.orderNotificationService = orderNotificationService;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemDefaultZone();
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> findAll(Authentication authentication, Pageable pageable, String userEmail, String email, UUID locationId, String status) {
        UserEntity user = resolveUser(authentication);
        String searchEmail = normalizeSearchEmail(userEmail, email);
        String normalizedStatus = normalizeOptionalStatus(status);
        if (locationId != null) {
            ensureLocationExists(locationId);
        }
        if (isAdmin(user.getId())) {
            return findAdminOrders(pageable, searchEmail, locationId, normalizedStatus);
        }

        List<UserPermissionEntity> permissions = userPermissionRepository
                .findByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrue(user.getId(), ORDER_MANAGE_PERMISSION);
        if (!permissions.isEmpty()) {
            return findManagerOrders(user, permissions, pageable, searchEmail, locationId, normalizedStatus);
        }
        if (isStoreRole(user.getId()) && user.getLocationId() != null) {
            return findStoreRoleOrders(user, pageable, searchEmail, locationId, normalizedStatus);
        }
        if (searchEmail != null && !user.getEmail().equalsIgnoreCase(searchEmail)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot view another user's order history");
        }
        if (locationId != null) {
            return (normalizedStatus == null
                    ? repository.findByUserIdAndLocationId(user.getId(), locationId, pageable)
                    : repository.findByUserIdAndLocationIdAndStatusIgnoreCase(user.getId(), locationId, normalizedStatus, pageable))
                    .map(this::toResponse);
        }
        return (normalizedStatus == null
                ? repository.findByUserId(user.getId(), pageable)
                : repository.findByUserIdAndStatusIgnoreCase(user.getId(), normalizedStatus, pageable))
                .map(this::toResponse);
    }

    private Page<OrderResponse> findAdminOrders(Pageable pageable, String userEmail, UUID locationId, String status) {
        if (userEmail == null || userEmail.isBlank()) {
            if (locationId != null) {
                return (status == null
                        ? repository.findByLocationId(locationId, pageable)
                        : repository.findByLocationIdAndStatusIgnoreCase(locationId, status, pageable))
                        .map(this::toResponse);
            }
            return (status == null ? repository.findAll(pageable) : repository.findByStatusIgnoreCase(status, pageable))
                    .map(this::toResponse);
        }
        return userRepository.findByEmail(userEmail)
                .map(targetUser -> locationId == null
                        ? (status == null
                                ? repository.findByUserId(targetUser.getId(), pageable)
                                : repository.findByUserIdAndStatusIgnoreCase(targetUser.getId(), status, pageable)).map(this::toResponse)
                        : (status == null
                                ? repository.findByUserIdAndLocationId(targetUser.getId(), locationId, pageable)
                                : repository.findByUserIdAndLocationIdAndStatusIgnoreCase(targetUser.getId(), locationId, status, pageable)).map(this::toResponse))
                .orElseGet(() -> Page.empty(pageable));
    }

    private Page<OrderResponse> findManagerOrders(
            UserEntity manager,
            List<UserPermissionEntity> permissions,
            Pageable pageable,
            String userEmail,
            UUID locationId,
            String status
    ) {
        if (permissions.stream().anyMatch(permission -> permission.getLocationId() == null)) {
            if (userEmail == null || userEmail.isBlank()) {
                if (locationId != null) {
                    return (status == null
                            ? repository.findByLocationId(locationId, pageable)
                            : repository.findByLocationIdAndStatusIgnoreCase(locationId, status, pageable))
                            .map(this::toResponse);
                }
                return (status == null ? repository.findAll(pageable) : repository.findByStatusIgnoreCase(status, pageable))
                        .map(this::toResponse);
            }
            return userRepository.findByEmail(userEmail)
                    .map(targetUser -> locationId == null
                            ? (status == null
                                    ? repository.findByUserId(targetUser.getId(), pageable)
                                    : repository.findByUserIdAndStatusIgnoreCase(targetUser.getId(), status, pageable)).map(this::toResponse)
                            : (status == null
                                    ? repository.findByUserIdAndLocationId(targetUser.getId(), locationId, pageable)
                                    : repository.findByUserIdAndLocationIdAndStatusIgnoreCase(targetUser.getId(), locationId, status, pageable)).map(this::toResponse))
                    .orElseGet(() -> Page.empty(pageable));
        }

        List<UUID> locationIds = permissions.stream()
                .map(UserPermissionEntity::getLocationId)
                .filter(permissionLocationId -> permissionLocationId != null)
                .distinct()
                .toList();
        if (locationId != null) {
            if (!locationIds.contains(locationId)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing order permission for location");
            }
            locationIds = List.of(locationId);
        }
        List<UUID> allowedLocationIds = locationIds;
        if (allowedLocationIds.isEmpty()) {
            return locationId == null
                    ? (status == null
                            ? repository.findByUserId(manager.getId(), pageable)
                            : repository.findByUserIdAndStatusIgnoreCase(manager.getId(), status, pageable)).map(this::toResponse)
                    : Page.empty(pageable);
        }
        if (userEmail == null || userEmail.isBlank()) {
            return (status == null
                    ? repository.findByLocationIdIn(allowedLocationIds, pageable)
                    : repository.findByLocationIdInAndStatusIgnoreCase(allowedLocationIds, status, pageable))
                    .map(this::toResponse);
        }
        return userRepository.findByEmail(userEmail)
                .map(targetUser -> (status == null
                        ? repository.findByUserIdAndLocationIdIn(targetUser.getId(), allowedLocationIds, pageable)
                        : repository.findByUserIdAndLocationIdInAndStatusIgnoreCase(targetUser.getId(), allowedLocationIds, status, pageable)).map(this::toResponse))
                .orElseGet(() -> Page.empty(pageable));
    }

    private Page<OrderResponse> findStoreRoleOrders(UserEntity storeUser, Pageable pageable, String userEmail, UUID locationId, String status) {
        if (locationId != null && !locationId.equals(storeUser.getLocationId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing order permission for location");
        }
        List<UUID> locationIds = List.of(storeUser.getLocationId());
        if (userEmail == null || userEmail.isBlank()) {
            return (status == null
                    ? repository.findByLocationIdIn(locationIds, pageable)
                    : repository.findByLocationIdInAndStatusIgnoreCase(locationIds, status, pageable))
                    .map(this::toResponse);
        }
        return userRepository.findByEmail(userEmail)
                .map(targetUser -> (status == null
                        ? repository.findByUserIdAndLocationIdIn(targetUser.getId(), locationIds, pageable)
                        : repository.findByUserIdAndLocationIdInAndStatusIgnoreCase(targetUser.getId(), locationIds, status, pageable)).map(this::toResponse))
                .orElseGet(() -> Page.empty(pageable));
    }

    private String normalizeOptionalStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return normalizeStatus(status);
    }

    private String normalizeSearchEmail(String userEmail, String email) {
        if (userEmail != null && !userEmail.isBlank()) {
            return userEmail.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        return null;
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Authentication authentication, UUID id) {
        OrderEntity order = findOrder(id);
        assertCanRead(authentication, order);
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderRedemptionPreviewResponse previewRedemption(Authentication authentication, OrderRedemptionPreviewRequest request) {
        UserEntity user = resolveUser(authentication);
        CartEntity cart = findCart(request.cartId());
        assertUserCart(user.getId(), cart);
        assertActiveCart(cart);
        return calculateRedemptionPreview(cart, user.getId(), request.pointsToRedeem(), request.tipAmount());
    }

    public OrderResponse checkout(Authentication authentication, OrderCheckoutRequest request) {
        UserEntity user = resolveUser(authentication);
        CartEntity cart = findCart(request.cartId());
        assertUserCart(user.getId(), cart);
        assertActiveCart(cart);
        ensureLocationExists(cart.getLocationId());

        List<CartItemEntity> cartItems = cartItemRepository.findByCart_Id(cart.getId());
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty");
        }

        RedemptionCalculation redemption = calculateRedemption(cart, user.getId(), request.pointsToRedeem());
        BigDecimal subtotal = cartItems.stream()
                .map(item -> nullToZero(item.getUnitPrice()).multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxableAmount = subtotal.subtract(redemption.amount()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxRate = settingDecimal(cart.getLocationId(), DEFAULT_TAX_RATE, BigDecimal.ZERO);
        BigDecimal taxAmount = taxableAmount.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal tipAmount = normalizeTipAmount(request.tipAmount());
        BigDecimal finalTotal = taxableAmount.add(taxAmount).add(tipAmount).setScale(2, RoundingMode.HALF_UP);

        String orderType = normalizeOrderType(request.orderType());
        LocalDateTime requestedPickupTime = resolveRequestedPickupTime(cart.getLocationId(), orderType, request.requestedPickupTime());

        OrderEntity order = new OrderEntity();
        order.setUserId(user.getId());
        order.setLocationId(cart.getLocationId());
        order.setAddressId(request.addressId());
        order.setOrderNumber(generateOrderNumber());
        order.setOrderType(orderType);
        order.setStatus(STATUS_PENDING);
        order.setSubtotal(subtotal);
        order.setTotalDiscount(redemption.amount());
        order.setTaxRate(taxRate);
        order.setTaxAmount(taxAmount);
        order.setTipAmount(tipAmount);
        order.setFinalTotal(finalTotal);
        order.setRequestedPickupTime(requestedPickupTime);
        order.setCustomerNote(trimToNull(request.customerNote()));
        order.setTaxExempt(false);
        order = repository.save(order);

        for (CartItemEntity cartItem : cartItems) {
            OrderItemEntity item = new OrderItemEntity();
            item.setOrderId(order.getId());
            item.setMenuItemId(cartItem.getMenuItemId());
            item.setItemName(cartItem.getItemName());
            item.setQuantity(cartItem.getQuantity());
            item.setUnitPrice(nullToZero(cartItem.getUnitPrice()));
            item.setImageUrl(cartItem.getImageUrl());
            item.setTotalPrice(nullToZero(cartItem.getUnitPrice()).multiply(BigDecimal.valueOf(cartItem.getQuantity())).setScale(2, RoundingMode.HALF_UP));
            item.setOptionSnapshot(readOptions(cartItem.getOptions()));
            orderItemRepository.save(item);
        }

        if (redemption.points() > 0) {
            OrderDiscountEntity discount = new OrderDiscountEntity();
            discount.setOrderId(order.getId());
            discount.setDiscountType("REWARD_POINTS");
            discount.setAmount(redemption.amount());
            discount.setMetadata(Map.of(
                    "pointsRedeemed", redemption.points(),
                    "pointValue", redemption.pointValue()
            ));
            orderDiscountRepository.save(discount);

            RewardRedemptionEntity redemptionEntity = new RewardRedemptionEntity();
            redemptionEntity.setUserId(user.getId());
            redemptionEntity.setOrderId(order.getId());
            redemptionEntity.setPointsRedeemed(redemption.points());
            redemptionEntity.setCashValue(redemption.amount());
            rewardRedemptionRepository.save(redemptionEntity);

            createRewardTransaction(user.getId(), order.getId(), "REDEEM", -redemption.points(), "ORDER", "Redeemed points on order " + order.getOrderNumber());
        }

        OrderTaxEntity tax = new OrderTaxEntity();
        tax.setOrderId(order.getId());
        tax.setTaxName("Sales Tax");
        tax.setTaxRate(taxRate);
        tax.setTaxableAmount(taxableAmount);
        tax.setTaxAmount(taxAmount);
        orderTaxRepository.save(tax);

        createStatusHistory(order.getId(), null, STATUS_PENDING, user.getId(), "Order created from cart");
        cart.setStatus(CHECKED_OUT_CART);
        cartRepository.save(cart);
        refreshWallet(user.getId());
        return toResponse(order);
    }

    public OrderResponse updateStatus(Authentication authentication, UUID id, OrderStatusUpdateRequest request) {
        OrderEntity order = findOrder(id);
        UserEntity user = resolveUser(authentication);
        assertCanManage(user, order.getLocationId());

        String newStatus = normalizeStatus(request.status());
        String oldStatus = order.getStatus();
        boolean pickupTimeChanged = request.requestedPickupTime() != null
                && (order.getRequestedPickupTime() == null || !request.requestedPickupTime().isEqual(order.getRequestedPickupTime()));
        if (newStatus.equalsIgnoreCase(oldStatus) && !pickupTimeChanged) {
            return toResponse(order);
        }

        if (request.requestedPickupTime() != null) {
            order.setRequestedPickupTime(resolveManagerRequestedPickupTime(order.getOrderType(), request.requestedPickupTime()));
        }
        order.setStatus(newStatus);
        order = repository.save(order);
        createStatusHistory(order.getId(), oldStatus, newStatus, user.getId(), trimToNull(request.note()));

        if (STATUS_PAID.equals(newStatus)) {
            awardPaidOrderPoints(order);
            awardReferralFirstOrderIfEligible(order);
            refreshWallet(order.getUserId());
        }

        OrderResponse response = toResponse(order);
        orderNotificationService.notifyStatusUpdated(response);
        return response;
    }

    public OrderResponse markPaidFromPayment(UUID id, UUID changedBy, String note) {
        OrderEntity order = findOrder(id);
        if (isPaidOrAcceptedStatus(order.getStatus())) {
            log.info("order already paid orderId={} orderNumber={} changedBy={}", order.getId(), order.getOrderNumber(), changedBy);
            return toResponse(order);
        }

        boolean autoAccepted = settingBoolean(order.getLocationId(), AUTO_ACCEPT_ORDERS, true);
        String newStatus = autoAccepted ? STATUS_PREPARING : STATUS_PAID;
        String oldStatus = order.getStatus();
        order.setStatus(newStatus);
        order = repository.save(order);
        createStatusHistory(order.getId(), oldStatus, newStatus, changedBy, trimToNull(note));
        log.info("order status changed from payment orderId={} orderNumber={} oldStatus={} newStatus={} changedBy={}",
                order.getId(), order.getOrderNumber(), oldStatus, newStatus, changedBy);
        awardPaidOrderPoints(order);
        awardReferralFirstOrderIfEligible(order);
        refreshWallet(order.getUserId());
        OrderResponse response = toResponse(order);
        orderNotificationService.notifyPaidOrder(response, autoAccepted);
        return response;
    }

    public void delete(Authentication authentication, UUID id) {
        OrderEntity order = findOrder(id);
        UserEntity user = resolveUser(authentication);
        assertCanManage(user, order.getLocationId());
        repository.delete(order);
    }

    private void awardPaidOrderPoints(OrderEntity order) {
        if (rewardTransactionRepository.existsByUserIdAndOrderIdAndType(order.getUserId(), order.getId(), "ORDER_EARN")) {
            return;
        }
        BigDecimal pointsPerDollar = settingDecimal(order.getLocationId(), POINTS_PER_DOLLAR, BigDecimal.ONE);
        int points = order.getSubtotal()
                .multiply(pointsPerDollar)
                .setScale(0, RoundingMode.DOWN)
                .intValue();
        if (points > 0) {
            createRewardTransaction(order.getUserId(), order.getId(), "ORDER_EARN", points, "ORDER", "Earned points from order " + order.getOrderNumber());
            log.info("order reward points awarded orderId={} orderNumber={} userId={} points={}",
                    order.getId(), order.getOrderNumber(), order.getUserId(), points);
        }
    }

    private void awardReferralFirstOrderIfEligible(OrderEntity order) {
        ReferralEntity referral = referralRepository.findFirstByReferredUserId(order.getUserId()).orElse(null);
        if (referral == null || "REWARDED".equalsIgnoreCase(referral.getStatus()) || "INVALID".equalsIgnoreCase(referral.getStatus())) {
            return;
        }
        long paidOrderCount = repository.countByUserIdAndStatusIn(
                order.getUserId(),
                List.of("PAID", "PREPARING", "READY", "COMPLETED")
        );
        if (paidOrderCount > 1) {
            referral.setStatus("INVALID");
            referralRepository.save(referral);
            log.info("referral marked invalid because paid order is not first orderId={} referredUserId={} referralId={}",
                    order.getId(), order.getUserId(), referral.getId());
            return;
        }
        BigDecimal minimum = settingDecimal(order.getLocationId(), MIN_REFERRAL_ORDER_AMOUNT, BigDecimal.valueOf(25));
        if (order.getSubtotal().compareTo(minimum) < 0) {
            referral.setStatus("INVALID");
            referralRepository.save(referral);
            log.info("referral marked invalid because order below minimum orderId={} referredUserId={} referralId={} subtotal={} minimum={}",
                    order.getId(), order.getUserId(), referral.getId(), order.getSubtotal(), minimum);
            return;
        }
        int points = settingDecimal(order.getLocationId(), REFERRAL_FIRST_ORDER_POINTS, BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.DOWN)
                .intValue();
        if (points > 0) {
            createRewardTransaction(referral.getReferrerId(), order.getId(), "REFERRAL_FIRST_ORDER", points, "REFERRAL", "Referral first order reward");
            referral.setStatus("REWARDED");
            referralRepository.save(referral);
            refreshWallet(referral.getReferrerId());
            log.info("referral first order points awarded orderId={} referralId={} referrerId={} referredUserId={} points={}",
                    order.getId(), referral.getId(), referral.getReferrerId(), order.getUserId(), points);
        }
    }

    private OrderRedemptionPreviewResponse calculateRedemptionPreview(CartEntity cart, UUID userId, Integer requestedPoints, BigDecimal requestedTipAmount) {
        RedemptionCalculation calculation = calculateRedemption(cart, userId, requestedPoints);
        BigDecimal subtotal = nullToZero(cart.getSubtotal()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxableAmount = subtotal.subtract(calculation.amount()).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxRate = settingDecimal(cart.getLocationId(), DEFAULT_TAX_RATE, BigDecimal.ZERO);
        BigDecimal taxAmount = taxableAmount.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal tipAmount = normalizeTipAmount(requestedTipAmount);
        return new OrderRedemptionPreviewResponse(
                cart.getId(),
                userId,
                cart.getLocationId(),
                calculation.availablePoints(),
                requestedPoints == null ? 0 : requestedPoints,
                calculation.points(),
                calculation.maxRedeemablePoints(),
                calculation.pointValue(),
                calculation.amount(),
                subtotal,
                taxableAmount,
                taxRate,
                taxAmount,
                tipAmount,
                taxableAmount.add(taxAmount).add(tipAmount).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private RedemptionCalculation calculateRedemption(CartEntity cart, UUID userId, Integer requestedPoints) {
        int requested = requestedPoints == null ? 0 : requestedPoints;
        if (requested < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "pointsToRedeem cannot be negative");
        }
        int available = Math.max(0, rewardTransactionRepository.sumPointsByUserId(userId));
        BigDecimal pointValue = settingDecimal(cart.getLocationId(), POINT_VALUE_CENTS, BigDecimal.valueOf(5))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (pointValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Point redemption value is not configured");
        }

        BigDecimal subtotal = nullToZero(cart.getSubtotal()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxPercent = settingDecimal(cart.getLocationId(), MAX_REDEMPTION_PERCENT, BigDecimal.valueOf(50));
        BigDecimal maxCash = subtotal.multiply(maxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        int maxByCash = maxCash.divide(pointValue, 0, RoundingMode.DOWN).intValue();
        int maxRedeemable = Math.max(0, Math.min(available, maxByCash));
        int applied = Math.min(requested, maxRedeemable);
        BigDecimal amount = pointValue.multiply(BigDecimal.valueOf(applied)).min(subtotal).setScale(2, RoundingMode.HALF_UP);
        return new RedemptionCalculation(available, requested, applied, maxRedeemable, pointValue, amount);
    }

    private BigDecimal settingDecimal(UUID locationId, String key, BigDecimal defaultValue) {
        String group = settingGroup(key);
        Optional<String> locationValue = locationId == null ? Optional.empty() : locationSettingRepository
                .findByLocationIdAndSettingGroupIgnoreCaseAndSettingKeyIgnoreCase(locationId, group, key)
                .map(setting -> setting.getSettingValue())
                .or(() -> locationSettingRepository.findByLocationIdAndSettingKeyIgnoreCase(locationId, key)
                        .map(setting -> setting.getSettingValue()));
        String value = locationValue
                .or(() -> systemSettingRepository.findBySettingGroupAndSettingKeyIgnoreCase(group, key)
                        .map(setting -> setting.getSettingValue()))
                .or(() -> systemSettingRepository.findBySettingKey(key)
                        .map(setting -> setting.getSettingValue()))
                .orElse(null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid setting value for " + key);
        }
    }

    private boolean settingBoolean(UUID locationId, String key, boolean defaultValue) {
        String group = settingGroup(key);
        Optional<String> locationValue = locationId == null ? Optional.empty() : locationSettingRepository
                .findByLocationIdAndSettingGroupIgnoreCaseAndSettingKeyIgnoreCase(locationId, group, key)
                .map(setting -> setting.getSettingValue())
                .or(() -> locationSettingRepository.findByLocationIdAndSettingKeyIgnoreCase(locationId, key)
                        .map(setting -> setting.getSettingValue()));
        String value = locationValue
                .or(() -> systemSettingRepository.findBySettingGroupAndSettingKeyIgnoreCase(group, key)
                        .map(setting -> setting.getSettingValue()))
                .or(() -> systemSettingRepository.findBySettingKey(key)
                        .map(setting -> setting.getSettingValue()))
                .orElse(null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String normalized = value.trim();
        if ("true".equalsIgnoreCase(normalized) || "1".equals(normalized) || "yes".equalsIgnoreCase(normalized)) {
            return true;
        }
        if ("false".equalsIgnoreCase(normalized) || "0".equals(normalized) || "no".equalsIgnoreCase(normalized)) {
            return false;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid setting value for " + key);
    }

    private String settingGroup(String key) {
        if (REFERRAL_FIRST_ORDER_POINTS.equals(key) || MIN_REFERRAL_ORDER_AMOUNT.equals(key)) {
            return "REFERRAL";
        }
        if (DEFAULT_TAX_RATE.equals(key) || MIN_PICKUP_TIME_MINUTES.equals(key) || AUTO_ACCEPT_ORDERS.equals(key)) {
            return "ORDER";
        }
        return "REWARD";
    }

    private OrderResponse toResponse(OrderEntity order) {
        List<OrderResponse.OrderItemResponse> items = orderItemRepository.findByOrderId(order.getId()).stream()
                .sorted(Comparator.comparing(OrderItemEntity::getCreatedAt))
                .map(item -> new OrderResponse.OrderItemResponse(
                        item.getId(),
                        item.getMenuItemId(),
                        item.getItemName(),
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getImageUrl(),
                        item.getTotalPrice(),
                        item.getOptionSnapshot()
                ))
                .toList();
        List<OrderResponse.OrderTaxResponse> taxes = orderTaxRepository.findByOrderId(order.getId()).stream()
                .map(tax -> new OrderResponse.OrderTaxResponse(tax.getId(), tax.getTaxName(), tax.getTaxRate(), tax.getTaxableAmount(), tax.getTaxAmount()))
                .toList();
        List<OrderResponse.OrderDiscountResponse> discounts = orderDiscountRepository.findByOrderId(order.getId()).stream()
                .map(discount -> new OrderResponse.OrderDiscountResponse(discount.getId(), discount.getDiscountType(), discount.getAmount(), discount.getMetadata()))
                .toList();
        RewardRedemptionEntity redemption = rewardRedemptionRepository.findByOrderId(order.getId()).orElse(null);
        Integer pointsEarned = rewardTransactionRepository.sumPointsByUserIdAndOrderIdAndType(order.getUserId(), order.getId(), "ORDER_EARN");
        CustomerSummary customer = resolveCustomerSummary(order.getUserId());
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                customer.name(),
                customer.email(),
                order.getLocationId(),
                order.getAddressId(),
                order.getOrderNumber(),
                order.getOrderType(),
                order.getStatus(),
                order.getSubtotal(),
                order.getTotalDiscount(),
                order.getTaxRate(),
                order.getTaxAmount(),
                nullToZero(order.getTipAmount()),
                order.getFinalTotal(),
                order.getRequestedPickupTime(),
                order.getCustomerNote(),
                order.getInternalNote(),
                redemption == null ? 0 : redemption.getPointsRedeemed(),
                redemption == null ? BigDecimal.ZERO : redemption.getCashValue(),
                pointsEarned,
                items,
                taxes,
                discounts,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private CustomerSummary resolveCustomerSummary(UUID userId) {
        if (userId == null) {
            return new CustomerSummary(null, null);
        }

        UserEntity user = userRepository.findById(userId).orElse(null);
        UserProfileEntity profile = userProfileRepository.findByUserId(userId).orElse(null);
        String name = buildCustomerName(profile);
        String email = user == null ? null : user.getEmail();

        return new CustomerSummary(name, email);
    }

    private String buildCustomerName(UserProfileEntity profile) {
        if (profile == null) {
            return null;
        }

        String firstName = profile.getFirstName() == null ? "" : profile.getFirstName().trim();
        String lastName = profile.getLastName() == null ? "" : profile.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();

        return fullName.isBlank() ? null : fullName;
    }

    private record CustomerSummary(String name, String email) {
    }

    private void assertCanRead(Authentication authentication, OrderEntity order) {
        UserEntity user = resolveUser(authentication);
        if (order.getUserId() != null && order.getUserId().equals(user.getId())) {
            return;
        }
        assertCanManage(user, order.getLocationId());
    }

    private void assertCanManage(UserEntity user, UUID locationId) {
        if (isAdmin(user.getId())) {
            return;
        }
        boolean global = userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(
                user.getId(), ORDER_MANAGE_PERMISSION
        );
        boolean location = locationId != null && userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(
                user.getId(), ORDER_MANAGE_PERMISSION, locationId
        );
        boolean ownStoreRole = locationId != null && isStoreRole(user.getId()) && locationId.equals(user.getLocationId());
        if (!global && !location && !ownStoreRole) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing order permission");
        }
    }

    private UserEntity resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    private boolean isAdmin(UUID userId) {
        return accountRoleService.resolveRoleNames(userId).contains("ROLE_ADMIN");
    }

    private boolean isStoreRole(UUID userId) {
        List<String> roleNames = accountRoleService.resolveRoleNames(userId);
        return roleNames.contains("ROLE_MANAGER") || roleNames.contains("ROLE_STAFF");
    }

    private CartEntity findCart(UUID cartId) {
        if (cartId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cartId is required");
        }
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + cartId));
    }

    private OrderEntity findOrder(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private void assertUserCart(UUID userId, CartEntity cart) {
        if (cart.getUserId() == null || !cart.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cart access denied");
        }
    }

    private void assertActiveCart(CartEntity cart) {
        if (!ACTIVE_CART.equalsIgnoreCase(cart.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is not active");
        }
    }

    private void ensureLocationExists(UUID locationId) {
        if (locationId == null || !locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Location not found: " + locationId);
        }
    }

    private void createRewardTransaction(UUID userId, UUID orderId, String type, int points, String source, String description) {
        RewardTransactionEntity transaction = new RewardTransactionEntity();
        transaction.setUserId(userId);
        transaction.setOrderId(orderId);
        transaction.setType(type);
        transaction.setPoints(points);
        transaction.setSource(source);
        transaction.setDescription(description);
        rewardTransactionRepository.save(transaction);
    }

    private void refreshWallet(UUID userId) {
        int balance = Math.max(0, rewardTransactionRepository.sumPointsByUserId(userId));
        int earned = rewardTransactionRepository.sumEarnedPointsByUserId(userId);
        int redeemed = rewardTransactionRepository.sumRedeemedPointsByUserId(userId);

        RewardWalletEntity wallet = rewardWalletRepository.findByUserId(userId).orElseGet(() -> {
            RewardWalletEntity entity = new RewardWalletEntity();
            entity.setUserId(userId);
            return entity;
        });
        wallet.setAvailableBalance(balance);
        wallet.setTotalEarned(earned);
        wallet.setTotalRedeemed(redeemed);
        rewardWalletRepository.save(wallet);
    }

    private void createStatusHistory(UUID orderId, String oldStatus, String newStatus, UUID changedBy, String note) {
        OrderStatusHistoryEntity history = new OrderStatusHistoryEntity();
        history.setOrderId(orderId);
        history.setOldStatus(oldStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);
        history.setNote(note);
        statusHistoryRepository.save(history);
    }

    private Map<String, Object> readOptions(String options) {
        if (options == null || options.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(options, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Map.of("raw", options);
        }
    }

    private String generateOrderNumber() {
        return "UM" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private String normalizeOrderType(String orderType) {
        String normalized = orderType == null || orderType.isBlank() ? "PICKUP" : orderType.trim().toUpperCase();
        if (!Set.of("PICKUP", "DELIVERY", "DINE_IN").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid orderType");
        }
        return normalized;
    }

    private LocalDateTime resolveRequestedPickupTime(UUID locationId, String orderType, LocalDateTime requestedPickupTime) {
        if (!"PICKUP".equals(orderType)) {
            return null;
        }

        int minimumMinutes = settingDecimal(locationId, MIN_PICKUP_TIME_MINUTES, BigDecimal.valueOf(15))
                .setScale(0, RoundingMode.CEILING)
                .intValue();
        if (minimumMinutes < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum pickup time cannot be negative");
        }

        LocalDateTime earliestPickupTime = LocalDateTime.now(clock).plusMinutes(minimumMinutes);
        LocalDateTime pickupTime = requestedPickupTime == null ? earliestPickupTime : requestedPickupTime;
        if (pickupTime.isBefore(earliestPickupTime)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestedPickupTime must be at least " + minimumMinutes + " minutes from now");
        }
        return pickupTime;
    }

    private LocalDateTime resolveManagerRequestedPickupTime(String orderType, LocalDateTime requestedPickupTime) {
        if (!"PICKUP".equals(orderType)) {
            return null;
        }
        return requestedPickupTime;
    }

    private String normalizeStatus(String status) {
        String normalized = status == null || status.isBlank() ? "" : status.trim().toUpperCase();
        if (!Set.of("PENDING", "PAID", "PREPARING", "READY", "COMPLETED", "CANCELLED").contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order status");
        }
        return normalized;
    }

    private boolean isPaidOrAcceptedStatus(String status) {
        return status != null && Set.of("PAID", "PREPARING", "READY", "COMPLETED").contains(status.trim().toUpperCase());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal normalizeTipAmount(BigDecimal tipAmount) {
        BigDecimal normalized = nullToZero(tipAmount).setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipAmount cannot be negative");
        }
        return normalized;
    }

    private record RedemptionCalculation(
            int availablePoints,
            int requestedPoints,
            int points,
            int maxRedeemablePoints,
            BigDecimal pointValue,
            BigDecimal amount
    ) {
    }
}
