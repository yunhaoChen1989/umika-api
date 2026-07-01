package ca.umika.api.store;

import ca.umika.api.admin.SystemSettingEntity;
import ca.umika.api.admin.SystemSettingRepository;
import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BusinessSettingService {

    private static final String MANAGE_PERMISSION_CODE = "LOCATION_SETTING_MANAGE";

    private static final Map<String, SettingDefinition> DEFINITIONS = Map.ofEntries(
            definition("ORDER", "DEFAULT_TAX_RATE", "Tax rate", "Sales tax percentage used for order checkout.", "decimal", "percent", "13"),
            definition("ORDER", "ORDER_DISCOUNT_PERCENT", "Order discount percent", "Default order-level discount percentage.", "decimal", "percent", "0"),
            definition("ORDER", "ORDER_DISCOUNT_AMOUNT", "Order discount amount", "Default fixed order-level discount amount.", "decimal", "currency", "0"),
            definition("REWARD", "POINTS_PER_DOLLAR", "Points per dollar", "Loyalty points earned per paid dollar.", "decimal", "points", "1"),
            definition("REWARD", "POINT_VALUE_CENTS", "Point value", "Cash redemption value of one point, in cents.", "decimal", "cents", "5"),
            definition("REWARD", "MAX_REDEMPTION_PERCENT", "Max redemption percent", "Maximum order subtotal percentage payable with points.", "decimal", "percent", "50"),
            definition("REWARD", "MIN_REDEEM_POINTS", "Minimum redeem points", "Smallest point block customers can redeem.", "integer", "points", "100"),
            definition("REWARD", "BIRTHDAY_BONUS_POINTS", "Birthday bonus points", "Points granted for birthday reward.", "integer", "points", "100"),
            definition("REFERRAL", "REFERRAL_SIGNUP_POINTS", "Referral signup points", "Points awarded when a referred user registers.", "integer", "points", "50"),
            definition("REFERRAL", "REFERRAL_FIRST_ORDER_POINTS", "Referral first-order points", "Points awarded when referred user places first qualifying order.", "integer", "points", "100"),
            definition("REFERRAL", "MIN_REFERRAL_ORDER_AMOUNT", "Referral minimum order", "Minimum order subtotal to trigger first-order referral reward.", "decimal", "currency", "25")
    );

    private final SystemSettingRepository systemSettingRepository;
    private final LocationSettingRepository locationSettingRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final AccountRoleService accountRoleService;

    public BusinessSettingService(
            SystemSettingRepository systemSettingRepository,
            LocationSettingRepository locationSettingRepository,
            LocationRepository locationRepository,
            UserRepository userRepository,
            UserPermissionRepository userPermissionRepository,
            AccountRoleService accountRoleService
    ) {
        this.systemSettingRepository = systemSettingRepository;
        this.locationSettingRepository = locationSettingRepository;
        this.locationRepository = locationRepository;
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.accountRoleService = accountRoleService;
    }

    @Transactional(readOnly = true)
    public BusinessSettingsResponse effective(Authentication authentication, UUID locationId, String locationCode) {
        UserEntity user = resolveUser(authentication);
        if (locationId == null && (locationCode == null || locationCode.isBlank())) {
            if (isStoreRole(user.getId()) && user.getLocationId() != null) {
                LocationEntity location = locationRepository.findById(user.getLocationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + user.getLocationId()));
                return buildResponse(location);
            }
            assertCanReadGlobal(user.getId());
            return new BusinessSettingsResponse(null, null, "Global", buildItems(null));
        }

        LocationEntity location = resolveLocation(user, locationId, locationCode);
        assertCanManage(user.getId(), location.getId());
        return buildResponse(location);
    }

    public BusinessSettingsResponse updateLocationSettings(Authentication authentication, UUID locationId, BusinessSettingsUpdateRequest request) {
        UserEntity user = resolveUser(authentication);
        LocationEntity location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationId));
        assertCanManage(user.getId(), location.getId());

        for (BusinessSettingValueRequest value : requireSettings(request)) {
            SettingDefinition definition = definitionFor(value.settingKey());
            String normalizedValue = normalizeAndValidateValue(definition, value.settingValue());
            LocationSettingEntity entity = locationSettingRepository
                    .findByLocationIdAndSettingGroupIgnoreCaseAndSettingKeyIgnoreCase(location.getId(), definition.group(), definition.key())
                    .orElseGet(() -> {
                        LocationSettingEntity created = new LocationSettingEntity();
                        created.setLocationId(location.getId());
                        created.setSettingGroup(definition.group());
                        created.setSettingKey(definition.key());
                        created.setDescription(definition.description());
                        return created;
                    });
            entity.setSettingValue(normalizedValue);
            entity.setDescription(definition.description());
            locationSettingRepository.save(entity);
        }

        return buildResponse(location);
    }

    public BusinessSettingsResponse resetLocationSetting(Authentication authentication, UUID locationId, String settingKey) {
        UserEntity user = resolveUser(authentication);
        LocationEntity location = locationRepository.findById(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationId));
        assertCanManage(user.getId(), location.getId());

        SettingDefinition definition = definitionFor(settingKey);
        locationSettingRepository
                .findByLocationIdAndSettingGroupIgnoreCaseAndSettingKeyIgnoreCase(location.getId(), definition.group(), definition.key())
                .ifPresent(locationSettingRepository::delete);
        return buildResponse(location);
    }

    public BusinessSettingsResponse updateSystemSettings(Authentication authentication, BusinessSettingsUpdateRequest request) {
        UserEntity user = resolveUser(authentication);
        accountRoleService.assertAdmin(user.getId());

        for (BusinessSettingValueRequest value : requireSettings(request)) {
            SettingDefinition definition = definitionFor(value.settingKey());
            String normalizedValue = normalizeAndValidateValue(definition, value.settingValue());
            SystemSettingEntity entity = systemSettingRepository
                    .findBySettingGroupAndSettingKeyIgnoreCase(definition.group(), definition.key())
                    .or(() -> systemSettingRepository.findBySettingKey(definition.key()))
                    .orElseGet(() -> {
                        SystemSettingEntity created = new SystemSettingEntity();
                        created.setSettingGroup(definition.group());
                        created.setSettingKey(definition.key());
                        created.setDescription(definition.description());
                        return created;
                    });
            entity.setSettingGroup(definition.group());
            entity.setSettingKey(definition.key());
            entity.setSettingValue(normalizedValue);
            entity.setDescription(definition.description());
            systemSettingRepository.save(entity);
        }

        LocationEntity location = locationRepository.findFirstByOrderByCreatedAtAsc().orElse(null);
        return location == null ? new BusinessSettingsResponse(null, null, null, buildItems(null)) : buildResponse(location);
    }

    private BusinessSettingsResponse buildResponse(LocationEntity location) {
        return new BusinessSettingsResponse(location.getId(), location.getLocationCode(), location.getName(), buildItems(location.getId()));
    }

    private List<BusinessSettingItemDto> buildItems(UUID locationId) {
        return DEFINITIONS.values().stream()
                .sorted(Comparator.comparing(SettingDefinition::group).thenComparing(SettingDefinition::key))
                .map(definition -> toItem(definition, locationId))
                .toList();
    }

    private BusinessSettingItemDto toItem(SettingDefinition definition, UUID locationId) {
        String systemValue = systemSettingRepository
                .findBySettingGroupAndSettingKeyIgnoreCase(definition.group(), definition.key())
                .or(() -> systemSettingRepository.findBySettingKey(definition.key()))
                .map(SystemSettingEntity::getSettingValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(definition.defaultValue());
        String locationValue = locationId == null ? null : locationSettingRepository
                .findByLocationIdAndSettingGroupIgnoreCaseAndSettingKeyIgnoreCase(locationId, definition.group(), definition.key())
                .map(LocationSettingEntity::getSettingValue)
                .filter(value -> value != null && !value.isBlank())
                .orElse(null);
        String effectiveValue = locationValue == null ? systemValue : locationValue;
        return new BusinessSettingItemDto(
                definition.group(),
                definition.key(),
                definition.label(),
                definition.description(),
                definition.valueType(),
                definition.unit(),
                systemValue,
                locationValue,
                effectiveValue,
                locationValue == null ? "SYSTEM" : "LOCATION"
        );
    }

    private LocationEntity resolveLocation(UserEntity user, UUID locationId, String locationCode) {
        if (locationId != null) {
            return locationRepository.findById(locationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationId));
        }
        if (locationCode != null && !locationCode.isBlank()) {
            return locationRepository.findByLocationCodeIgnoreCase(locationCode.trim().toUpperCase(Locale.ROOT))
                    .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationCode));
        }
        if (user.getLocationId() != null) {
            return locationRepository.findById(user.getLocationId())
                    .orElseGet(this::firstLocation);
        }
        return firstLocation();
    }

    private LocationEntity firstLocation() {
        return locationRepository.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new ResourceNotFoundException("No locations found"));
    }

    private void assertCanManage(UUID userId, UUID locationId) {
        List<String> roleNames = accountRoleService.resolveRoleNames(userId);
        if (roleNames.contains("ROLE_ADMIN")) {
            return;
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        boolean global = userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(
                userId, MANAGE_PERMISSION_CODE
        );
        boolean location = userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(
                userId, MANAGE_PERMISSION_CODE, locationId
        );
        boolean ownStoreRole = (roleNames.contains("ROLE_MANAGER") || roleNames.contains("ROLE_STAFF"))
                && locationId != null
                && locationId.equals(user.getLocationId());
        if (!global && !location && !ownStoreRole) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing business setting permission");
        }
    }

    private void assertCanReadGlobal(UUID userId) {
        if (accountRoleService.resolveRoleNames(userId).contains("ROLE_ADMIN")) {
            return;
        }
        boolean global = userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(
                userId, MANAGE_PERMISSION_CODE
        );
        if (!global) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing business setting permission");
        }
    }

    private boolean isStoreRole(UUID userId) {
        List<String> roleNames = accountRoleService.resolveRoleNames(userId);
        return roleNames.contains("ROLE_MANAGER") || roleNames.contains("ROLE_STAFF");
    }

    private UserEntity resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    private List<BusinessSettingValueRequest> requireSettings(BusinessSettingsUpdateRequest request) {
        if (request == null || request.settings() == null || request.settings().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "settings are required");
        }
        return request.settings();
    }

    private SettingDefinition definitionFor(String settingKey) {
        if (settingKey == null || settingKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "settingKey is required");
        }
        SettingDefinition definition = DEFINITIONS.get(settingKey.trim().toUpperCase(Locale.ROOT));
        if (definition == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported settingKey: " + settingKey);
        }
        return definition;
    }

    private String normalizeAndValidateValue(SettingDefinition definition, String settingValue) {
        if (settingValue == null || settingValue.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, definition.key() + " value is required");
        }
        String trimmed = settingValue.trim();
        BigDecimal value;
        try {
            value = new BigDecimal(trimmed);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, definition.key() + " must be numeric");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, definition.key() + " cannot be negative");
        }
        if ("integer".equals(definition.valueType()) && value.stripTrailingZeros().scale() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, definition.key() + " must be a whole number");
        }
        if ("percent".equals(definition.unit()) && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, definition.key() + " cannot be greater than 100");
        }
        return value.stripTrailingZeros().toPlainString();
    }

    private static Map.Entry<String, SettingDefinition> definition(
            String group,
            String key,
            String label,
            String description,
            String valueType,
            String unit,
            String defaultValue
    ) {
        SettingDefinition definition = new SettingDefinition(group, key, label, description, valueType, unit, defaultValue);
        return Map.entry(key, definition);
    }

    private record SettingDefinition(
            String group,
            String key,
            String label,
            String description,
            String valueType,
            String unit,
            String defaultValue
    ) {
    }
}
