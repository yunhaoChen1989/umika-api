package ca.umika.api.reward;

import ca.umika.api.admin.SystemSettingEntity;
import ca.umika.api.admin.SystemSettingRepository;
import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.order.OrderEntity;
import ca.umika.api.order.OrderRepository;
import ca.umika.api.store.LocationEntity;
import ca.umika.api.store.LocationRepository;
import ca.umika.api.store.LocationSettingEntity;
import ca.umika.api.store.LocationSettingRepository;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CurrentUserRewardsService {

    private static final String POINTS_PER_DOLLAR = "POINTS_PER_DOLLAR";
    private static final String POINT_VALUE_CENTS = "POINT_VALUE_CENTS";
    private static final String MAX_REDEMPTION_PERCENT = "MAX_REDEMPTION_PERCENT";
    private static final String BIRTHDAY_BONUS_POINTS = "BIRTHDAY_BONUS_POINTS";
    private static final String REFERRAL_SIGNUP_POINTS = "REFERRAL_SIGNUP_POINTS";
    private static final String REFERRAL_FIRST_ORDER_POINTS = "REFERRAL_FIRST_ORDER_POINTS";
    private static final String MIN_REFERRAL_ORDER_AMOUNT = "MIN_REFERRAL_ORDER_AMOUNT";
    private static final String MIN_REDEEM_POINTS = "MIN_REDEEM_POINTS";

    private final UserRepository userRepository;
    private final RewardTransactionRepository rewardTransactionRepository;
    private final OrderRepository orderRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final LocationSettingRepository locationSettingRepository;
    private final LocationRepository locationRepository;
    private final String publicBaseUrl;

    public CurrentUserRewardsService(
            UserRepository userRepository,
            RewardTransactionRepository rewardTransactionRepository,
            OrderRepository orderRepository,
            SystemSettingRepository systemSettingRepository,
            LocationSettingRepository locationSettingRepository,
            LocationRepository locationRepository,
            @Value("${app.public-base-url:https://umikasushi.ca}") String publicBaseUrl
    ) {
        this.userRepository = userRepository;
        this.rewardTransactionRepository = rewardTransactionRepository;
        this.orderRepository = orderRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.locationSettingRepository = locationSettingRepository;
        this.locationRepository = locationRepository;
        this.publicBaseUrl = publicBaseUrl;
    }

    public CurrentUserRewardsDto summary(Authentication authentication, UUID locationId, String locationCode) {
        UserEntity user = resolveUser(authentication);
        UUID resolvedLocationId = resolveLocationId(locationId, locationCode);
        int balance = Math.max(0, rewardTransactionRepository.sumPointsByUserId(user.getId()));
        int earned = rewardTransactionRepository.sumEarnedPointsByUserId(user.getId());
        int redeemed = rewardTransactionRepository.sumRedeemedPointsByUserId(user.getId());
        BigDecimal pointValueCents = settingDecimal(resolvedLocationId, POINT_VALUE_CENTS, BigDecimal.valueOf(5));
        int minimumRedeemPoints = settingInteger(resolvedLocationId, MIN_REDEEM_POINTS, 100);

        return new CurrentUserRewardsDto(
                balance,
                earned,
                redeemed,
                pointValueCents,
                settingDecimal(resolvedLocationId, POINTS_PER_DOLLAR, BigDecimal.ONE),
                settingDecimal(resolvedLocationId, MAX_REDEMPTION_PERCENT, BigDecimal.valueOf(50)),
                minimumRedeemPoints,
                pointsToDollars(minimumRedeemPoints, pointValueCents),
                settingInteger(resolvedLocationId, BIRTHDAY_BONUS_POINTS, 100),
                settingInteger(resolvedLocationId, REFERRAL_SIGNUP_POINTS, 50),
                settingInteger(resolvedLocationId, REFERRAL_FIRST_ORDER_POINTS, 100),
                settingDecimal(resolvedLocationId, MIN_REFERRAL_ORDER_AMOUNT, BigDecimal.valueOf(25)),
                user.getReferralCode(),
                buildReferralInviteUrl(user.getReferralCode())
        );
    }

    public Page<CurrentUserRewardTransactionDto> transactions(Authentication authentication, Pageable pageable) {
        UserEntity user = resolveUser(authentication);
        Page<RewardTransactionEntity> transactions = rewardTransactionRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);
        Map<UUID, String> orderNumbers = resolveOrderNumbers(transactions);
        return transactions.map(transaction -> toTransactionDto(transaction, orderNumbers));
    }

    public CurrentUserRewardRedemptionStatusDto redemptionStatus(Authentication authentication, UUID locationId, String locationCode) {
        UserEntity user = resolveUser(authentication);
        UUID resolvedLocationId = resolveLocationId(locationId, locationCode);
        int balance = Math.max(0, rewardTransactionRepository.sumPointsByUserId(user.getId()));
        int minimumRedeemPoints = settingInteger(resolvedLocationId, MIN_REDEEM_POINTS, 100);
        BigDecimal pointValueCents = settingDecimal(resolvedLocationId, POINT_VALUE_CENTS, BigDecimal.valueOf(5));
        int redeemablePoints = balance < minimumRedeemPoints ? 0 : (balance / minimumRedeemPoints) * minimumRedeemPoints;
        return new CurrentUserRewardRedemptionStatusDto(
                balance,
                redeemablePoints,
                pointsToDollars(redeemablePoints, pointValueCents),
                minimumRedeemPoints,
                pointValueCents
        );
    }

    private Map<UUID, String> resolveOrderNumbers(Page<RewardTransactionEntity> transactions) {
        var orderIds = transactions.stream()
                .map(RewardTransactionEntity::getOrderId)
                .filter(orderId -> orderId != null)
                .distinct()
                .toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        return orderRepository.findAllById(orderIds).stream()
                .collect(Collectors.toMap(
                        OrderEntity::getId,
                        OrderEntity::getOrderNumber,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private CurrentUserRewardTransactionDto toTransactionDto(RewardTransactionEntity transaction, Map<UUID, String> orderNumbers) {
        return new CurrentUserRewardTransactionDto(
                transaction.getId(),
                normalizeTransactionType(transaction.getType()),
                transaction.getPoints(),
                transaction.getDescription(),
                transaction.getOrderId(),
                transaction.getOrderId() == null ? null : orderNumbers.get(transaction.getOrderId()),
                transaction.getCreatedAt()
        );
    }

    private String normalizeTransactionType(String type) {
        if (type == null || type.isBlank()) {
            return "ADMIN_ADJUSTMENT";
        }
        return switch (type.trim().toUpperCase(Locale.ROOT)) {
            case "ORDER_EARN", "ORDER" -> "ORDER";
            case "REDEEM", "REDEMPTION" -> "REDEEM";
            case "REFERRAL_SIGNUP", "REFERRAL_REGISTER" -> "REFERRAL_REGISTER";
            case "REFERRAL_FIRST_ORDER" -> "REFERRAL_FIRST_ORDER";
            case "BIRTHDAY", "BIRTHDAY_BONUS" -> "BIRTHDAY_BONUS";
            case "ADJUSTMENT", "ADMIN_ADJUSTMENT" -> "ADMIN_ADJUSTMENT";
            case "EXPIRED" -> "EXPIRED";
            case "CANCELLED_ORDER_REVERSAL" -> "CANCELLED_ORDER_REVERSAL";
            default -> type.trim().toUpperCase(Locale.ROOT);
        };
    }

    private UUID resolveLocationId(UUID locationId, String locationCode) {
        if (locationId != null) {
            if (!locationRepository.existsById(locationId)) {
                throw new ResourceNotFoundException("Location not found: " + locationId);
            }
            return locationId;
        }
        if (locationCode == null || locationCode.isBlank()) {
            return null;
        }
        return locationRepository.findByLocationCodeIgnoreCase(locationCode.trim().toUpperCase(Locale.ROOT))
                .map(LocationEntity::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationCode));
    }

    private BigDecimal settingDecimal(UUID locationId, String key, BigDecimal defaultValue) {
        String value = settingValue(locationId, key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reward setting value for " + key);
        }
    }

    private int settingInteger(UUID locationId, String key, int defaultValue) {
        return settingDecimal(locationId, key, BigDecimal.valueOf(defaultValue))
                .setScale(0, RoundingMode.DOWN)
                .intValue();
    }

    private String settingValue(UUID locationId, String key) {
        return (locationId == null ? java.util.Optional.<String>empty() : locationSettingRepository
                .findByLocationIdAndSettingKeyIgnoreCase(locationId, key)
                .map(LocationSettingEntity::getSettingValue))
                .or(() -> systemSettingRepository.findBySettingKey(key).map(SystemSettingEntity::getSettingValue))
                .or(() -> systemSettingRepository.findBySettingGroupAndSettingKeyIgnoreCase("REWARD", key).map(SystemSettingEntity::getSettingValue))
                .or(() -> systemSettingRepository.findBySettingGroupAndSettingKeyIgnoreCase("REFERRAL", key).map(SystemSettingEntity::getSettingValue))
                .orElse(null);
    }

    private BigDecimal pointsToDollars(int points, BigDecimal pointValueCents) {
        return pointValueCents
                .multiply(BigDecimal.valueOf(points))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String buildReferralInviteUrl(String referralCode) {
        if (referralCode == null || referralCode.isBlank()) {
            return null;
        }
        String base = publicBaseUrl == null || publicBaseUrl.isBlank() ? "https://umikasushi.ca" : publicBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/register?ref=" + referralCode;
    }

    private UserEntity resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }
}
