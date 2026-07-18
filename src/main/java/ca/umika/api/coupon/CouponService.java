package ca.umika.api.coupon;

import ca.umika.api.admin.UserPermissionEntity;
import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.order.OrderRepository;
import ca.umika.api.store.LocationRepository;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CouponService {

    public static final String COUPON_VIEW = "COUPON_VIEW";
    public static final String COUPON_CREATE = "COUPON_CREATE";
    public static final String COUPON_UPDATE = "COUPON_UPDATE";
    public static final String COUPON_STATUS_UPDATE = "COUPON_STATUS_UPDATE";
    public static final String COUPON_GLOBAL_MANAGE = "COUPON_GLOBAL_MANAGE";

    private static final String PERCENT = "PERCENT";
    private static final String FIXED_AMOUNT = "FIXED_AMOUNT";
    private static final List<String> COUNTED_REDEMPTION_STATUSES = List.of("RESERVED", "APPLIED");
    private static final List<String> PAID_ORDER_STATUSES = List.of("PAID", "PREPARING", "READY", "COMPLETED");

    private final CouponRepository repository;
    private final CouponRedemptionRepository redemptionRepository;
    private final CouponMapper mapper;
    private final UserRepository userRepository;
    private final AccountRoleService accountRoleService;
    private final UserPermissionRepository userPermissionRepository;
    private final LocationRepository locationRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;

    public CouponService(
            CouponRepository repository,
            CouponRedemptionRepository redemptionRepository,
            CouponMapper mapper,
            UserRepository userRepository,
            AccountRoleService accountRoleService,
            UserPermissionRepository userPermissionRepository,
            LocationRepository locationRepository,
            OrderRepository orderRepository
    ) {
        this.repository = repository;
        this.redemptionRepository = redemptionRepository;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.accountRoleService = accountRoleService;
        this.userPermissionRepository = userPermissionRepository;
        this.locationRepository = locationRepository;
        this.orderRepository = orderRepository;
        this.clock = Clock.systemDefaultZone();
    }

    @Transactional(readOnly = true)
    public Page<CouponDto> findAll(Authentication authentication, Pageable pageable, UUID locationId) {
        UserEntity user = resolveUser(authentication);
        if (isAdmin(user.getId())) {
            if (locationId != null) {
                ensureLocationExists(locationId);
                return repository.findByLocationId(locationId, pageable).map(mapper::toDto);
            }
            return repository.findAll(pageable).map(mapper::toDto);
        }

        if (locationId != null) {
            assertLocationPermission(user, COUPON_VIEW, locationId);
            return repository.findByLocationId(locationId, pageable).map(mapper::toDto);
        }

        List<UUID> locationIds = permittedLocationIds(user.getId(), COUPON_VIEW);
        if (locationIds.isEmpty()) {
            throw forbidden("Missing coupon permission");
        }
        List<CouponDto> coupons = repository.findAll(pageable.getSort()).stream()
                .filter(coupon -> coupon.getLocationId() != null && locationIds.contains(coupon.getLocationId()))
                .map(mapper::toDto)
                .toList();
        int start = Math.min((int) pageable.getOffset(), coupons.size());
        int end = Math.min(start + pageable.getPageSize(), coupons.size());
        return new PageImpl<>(coupons.subList(start, end), pageable, coupons.size());
    }

    @Transactional(readOnly = true)
    public CouponDto findById(Authentication authentication, UUID id) {
        UserEntity user = resolveUser(authentication);
        CouponEntity coupon = findCoupon(id);
        assertCanView(user, coupon);
        return mapper.toDto(coupon);
    }

    public CouponDto create(Authentication authentication, CouponDto dto) {
        UserEntity user = resolveUser(authentication);
        validateDto(dto, null);
        assertCanManage(user, dto.locationId(), COUPON_CREATE, true);
        CouponEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        entity.setCode(normalizeCode(dto.code()));
        entity.setDiscountType(normalizeDiscountType(dto.discountType()));
        entity.setIsActive(dto.isActive() == null ? Boolean.TRUE : dto.isActive());
        entity.setFirstOrderOnly(Boolean.TRUE.equals(dto.firstOrderOnly()));
        entity.setNewCustomerOnly(Boolean.TRUE.equals(dto.newCustomerOnly()));
        return mapper.toDto(repository.save(entity));
    }

    public CouponDto update(Authentication authentication, UUID id, CouponDto dto) {
        UserEntity user = resolveUser(authentication);
        CouponEntity entity = findCoupon(id);
        validateDto(dto, id);
        assertCanManage(user, entity.getLocationId(), COUPON_UPDATE, false);
        assertCanManage(user, dto.locationId(), COUPON_UPDATE, false);
        mapper.updateEntity(entity, dto);
        entity.setCode(normalizeCode(dto.code()));
        entity.setDiscountType(normalizeDiscountType(dto.discountType()));
        entity.setFirstOrderOnly(Boolean.TRUE.equals(dto.firstOrderOnly()));
        entity.setNewCustomerOnly(Boolean.TRUE.equals(dto.newCustomerOnly()));
        entity.setIsActive(dto.isActive() == null ? Boolean.TRUE : dto.isActive());
        return mapper.toDto(repository.save(entity));
    }

    public CouponDto updateStatus(Authentication authentication, UUID id, CouponStatusRequest request) {
        if (request == null || request.isActive() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "isActive is required");
        }
        UserEntity user = resolveUser(authentication);
        CouponEntity entity = findCoupon(id);
        assertCanManage(user, entity.getLocationId(), COUPON_STATUS_UPDATE, false);
        entity.setIsActive(request.isActive());
        return mapper.toDto(repository.save(entity));
    }

    @Transactional(readOnly = true)
    public CouponCalculation calculate(String couponCode, UUID locationId, UUID userId, BigDecimal subtotal) {
        if (couponCode == null || couponCode.isBlank()) {
            return CouponCalculation.none();
        }
        if (locationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "locationId is required for coupon validation");
        }
        CouponEntity coupon = repository.findByCodeIgnoreCase(couponCode.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon code is invalid"));
        validateCoupon(coupon, locationId, userId, subtotal);
        BigDecimal discount = calculateDiscount(coupon, subtotal);
        return new CouponCalculation(coupon, coupon.getCode(), discount, "Coupon applied");
    }

    public CouponRedemptionEntity createRedemption(CouponCalculation calculation, UUID userId, UUID orderId, UUID locationId, BigDecimal subtotal) {
        if (calculation == null || calculation.coupon() == null || calculation.discountAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        CouponRedemptionEntity redemption = new CouponRedemptionEntity();
        redemption.setCouponId(calculation.coupon().getId());
        redemption.setUserId(userId);
        redemption.setOrderId(orderId);
        redemption.setLocationId(locationId);
        redemption.setCouponCodeSnapshot(calculation.couponCode());
        redemption.setDiscountAmount(calculation.discountAmount());
        redemption.setOrderSubtotalSnapshot(subtotal);
        redemption.setStatus("RESERVED");
        redemption.setRedeemedAt(LocalDateTime.now(clock));
        return redemptionRepository.save(redemption);
    }

    public void markOrderRedemptionsApplied(UUID orderId) {
        if (orderId == null) {
            return;
        }
        redemptionRepository.findByOrderId(orderId).forEach(redemption -> {
            if ("RESERVED".equalsIgnoreCase(redemption.getStatus())) {
                redemption.setStatus("APPLIED");
                redemption.setRedeemedAt(LocalDateTime.now(clock));
                redemptionRepository.save(redemption);
            }
        });
    }

    public void markOrderRedemptionsRefunded(UUID orderId) {
        if (orderId == null) {
            return;
        }
        redemptionRepository.findByOrderId(orderId).forEach(redemption -> {
            if ("RESERVED".equalsIgnoreCase(redemption.getStatus())
                    || "APPLIED".equalsIgnoreCase(redemption.getStatus())) {
                redemption.setStatus("REFUNDED");
                redemptionRepository.save(redemption);
            }
        });
    }

    private void validateCoupon(CouponEntity coupon, UUID locationId, UUID userId, BigDecimal subtotal) {
        BigDecimal resolvedSubtotal = nullToZero(subtotal).setScale(2, RoundingMode.HALF_UP);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!Boolean.TRUE.equals(coupon.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is inactive");
        }
        if (coupon.getLocationId() != null && !coupon.getLocationId().equals(locationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is not valid for this location");
        }
        if (coupon.getStartsAt() != null && coupon.getStartsAt().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is not active yet");
        }
        if (coupon.getEndsAt() != null && coupon.getEndsAt().isBefore(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon has expired");
        }
        if (coupon.getMinimumOrderAmount() != null && resolvedSubtotal.compareTo(coupon.getMinimumOrderAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order subtotal does not meet coupon minimum");
        }
        if (userId != null && Boolean.TRUE.equals(coupon.getFirstOrderOnly())
                && orderRepository.countByUserIdAndStatusIn(userId, PAID_ORDER_STATUSES) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is only valid for first orders");
        }
        if (userId != null && Boolean.TRUE.equals(coupon.getNewCustomerOnly())
                && orderRepository.countByUserIdAndStatusIn(userId, PAID_ORDER_STATUSES) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon is only valid for new customers");
        }
        if (coupon.getUsageLimitTotal() != null
                && redemptionRepository.countByCouponIdAndStatusIn(coupon.getId(), COUNTED_REDEMPTION_STATUSES) >= coupon.getUsageLimitTotal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon usage limit reached");
        }
        if (userId != null && coupon.getUsageLimitPerUser() != null
                && redemptionRepository.countByCouponIdAndUserIdAndStatusIn(coupon.getId(), userId, COUNTED_REDEMPTION_STATUSES) >= coupon.getUsageLimitPerUser()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon usage limit reached for this user");
        }
    }

    private BigDecimal calculateDiscount(CouponEntity coupon, BigDecimal subtotal) {
        BigDecimal resolvedSubtotal = nullToZero(subtotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount;
        if (PERCENT.equals(coupon.getDiscountType())) {
            discount = resolvedSubtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaximumDiscountAmount() != null) {
                discount = discount.min(coupon.getMaximumDiscountAmount());
            }
        } else if (FIXED_AMOUNT.equals(coupon.getDiscountType())) {
            discount = coupon.getDiscountValue();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported coupon discount type");
        }
        return discount.max(BigDecimal.ZERO).min(resolvedSubtotal).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateDto(CouponDto dto, UUID existingId) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coupon payload is required");
        }
        if (dto.code() == null || dto.code().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "code is required");
        }
        String normalizedCode = normalizeCode(dto.code());
        repository.findByCodeIgnoreCase(normalizedCode)
                .filter(existing -> !existing.getId().equals(existingId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Coupon code already exists");
                });
        if (dto.name() == null || dto.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        String discountType = normalizeDiscountType(dto.discountType());
        if (!Set.of(PERCENT, FIXED_AMOUNT).contains(discountType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discountType must be PERCENT or FIXED_AMOUNT");
        }
        if (dto.discountValue() == null || dto.discountValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "discountValue must be greater than or equal to 0");
        }
        if (PERCENT.equals(discountType) && dto.discountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "percent discountValue cannot be greater than 100");
        }
        if (dto.locationId() != null) {
            ensureLocationExists(dto.locationId());
        }
        if (dto.startsAt() != null && dto.endsAt() != null && dto.endsAt().isBefore(dto.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endsAt must be after startsAt");
        }
    }

    private void assertCanView(UserEntity user, CouponEntity coupon) {
        if (isAdmin(user.getId())) {
            return;
        }
        if (coupon.getLocationId() == null) {
            throw forbidden("Missing global coupon permission");
        }
        assertLocationPermission(user, COUPON_VIEW, coupon.getLocationId());
    }

    private void assertCanManage(UserEntity user, UUID locationId, String permissionCode, boolean creating) {
        if (isAdmin(user.getId())) {
            return;
        }
        if (locationId == null) {
            throw forbidden(creating ? "Managers cannot create global coupons" : "Missing global coupon permission");
        }
        assertLocationPermission(user, permissionCode, locationId);
    }

    private void assertLocationPermission(UserEntity user, String permissionCode, UUID locationId) {
        if (!isManager(user.getId())) {
            throw forbidden("Manager role required");
        }
        ensureLocationExists(locationId);
        boolean permitted = userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(
                user.getId(), permissionCode, locationId
        );
        if (!permitted) {
            throw forbidden("Missing coupon permission");
        }
    }

    private List<UUID> permittedLocationIds(UUID userId, String permissionCode) {
        return userPermissionRepository.findByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrue(userId, permissionCode).stream()
                .map(UserPermissionEntity::getLocationId)
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    private CouponEntity findCoupon(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + id));
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

    private boolean isManager(UUID userId) {
        return accountRoleService.resolveRoleNames(userId).contains("ROLE_MANAGER");
    }

    private void ensureLocationExists(UUID locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Location not found: " + locationId);
        }
    }

    private ResponseStatusException forbidden(String message) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private String normalizeDiscountType(String discountType) {
        return discountType == null ? null : discountType.trim().toUpperCase();
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
