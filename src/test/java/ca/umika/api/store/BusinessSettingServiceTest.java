package ca.umika.api.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.umika.api.admin.SystemSettingRepository;
import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

class BusinessSettingServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final Authentication AUTHENTICATION = UsernamePasswordAuthenticationToken.authenticated(
            "manager@umika.test", "n/a", List.of());

    private SystemSettingRepository systemSettingRepository;
    private LocationSettingRepository locationSettingRepository;
    private LocationRepository locationRepository;
    private UserRepository userRepository;
    private UserPermissionRepository userPermissionRepository;
    private AccountRoleService accountRoleService;
    private BusinessSettingService service;
    private UserEntity user;
    private LocationEntity location;

    @BeforeEach
    void setUp() {
        systemSettingRepository = mock(SystemSettingRepository.class);
        locationSettingRepository = mock(LocationSettingRepository.class);
        locationRepository = mock(LocationRepository.class);
        userRepository = mock(UserRepository.class);
        userPermissionRepository = mock(UserPermissionRepository.class);
        accountRoleService = mock(AccountRoleService.class);
        service = new BusinessSettingService(
                systemSettingRepository,
                locationSettingRepository,
                locationRepository,
                userRepository,
                userPermissionRepository,
                accountRoleService
        );
        user = new UserEntity();
        user.setId(USER_ID);
        user.setEmail("manager@umika.test");
        location = new LocationEntity();
        location.setId(LOCATION_ID);
        location.setLocationCode("UM001");
        location.setName("Umika Test");
        when(userRepository.findByEmail("manager@umika.test")).thenReturn(Optional.of(user));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(locationRepository.findById(LOCATION_ID)).thenReturn(Optional.of(location));
    }

    @Test
    void effectiveResponseExposesPickupSettingMetadataAndDefaults() {
        when(accountRoleService.resolveRoleNames(USER_ID)).thenReturn(List.of("ROLE_ADMIN"));

        BusinessSettingsResponse response = service.effective(AUTHENTICATION, null, null);

        BusinessSettingItemDto item = response.settings().stream()
                .filter(setting -> setting.settingKey().equals("PICKUP_TIME_80_TO_100_MINUTES"))
                .findFirst()
                .orElseThrow();
        assertThat(item.settingGroup()).isEqualTo("ORDER");
        assertThat(item.label()).isEqualTo("Pickup time from $80 to $100");
        assertThat(item.description()).contains("final total");
        assertThat(item.valueType()).isEqualTo("integer");
        assertThat(item.unit()).isEqualTo("minutes");
        assertThat(item.effectiveValue()).isEqualTo("25");
        assertThat(item.source()).isEqualTo("SYSTEM");
    }

    @Test
    void managerWithLocationPermissionCanUpdatePickupTier() {
        when(accountRoleService.resolveRoleNames(USER_ID)).thenReturn(List.of("ROLE_MANAGER"));
        when(userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(
                USER_ID, "LOCATION_SETTING_MANAGE", LOCATION_ID)).thenReturn(true);

        service.updateLocationSettings(
                AUTHENTICATION,
                LOCATION_ID,
                new BusinessSettingsUpdateRequest(List.of(
                        new BusinessSettingValueRequest("PICKUP_TIME_OVER_100_MINUTES", "40")
                ))
        );

        verify(locationSettingRepository).save(any(LocationSettingEntity.class));
    }

    @Test
    void publicLocationSettingsExposeDeliveryAvailability() {
        LocationSettingEntity deliverySetting = new LocationSettingEntity();
        deliverySetting.setLocationId(LOCATION_ID);
        deliverySetting.setSettingGroup("ORDER");
        deliverySetting.setSettingKey("DELIVERY_ENABLED");
        deliverySetting.setSettingValue("false");
        when(locationSettingRepository.findByLocationIdAndSettingGroupIgnoreCaseAndSettingKeyIgnoreCase(
                LOCATION_ID, "ORDER", "DELIVERY_ENABLED")).thenReturn(Optional.of(deliverySetting));

        BusinessSettingsResponse response = service.effective(null, LOCATION_ID, null);

        BusinessSettingItemDto item = response.settings().stream()
                .filter(setting -> setting.settingKey().equals("DELIVERY_ENABLED"))
                .findFirst()
                .orElseThrow();
        assertThat(item.valueType()).isEqualTo("boolean");
        assertThat(item.effectiveValue()).isEqualTo("false");
        assertThat(item.source()).isEqualTo("LOCATION");
    }

    @Test
    void rejectsZeroAndFractionalPickupMinutes() {
        when(accountRoleService.resolveRoleNames(USER_ID)).thenReturn(List.of("ROLE_ADMIN"));

        assertThatThrownBy(() -> service.updateSystemSettings(
                AUTHENTICATION,
                new BusinessSettingsUpdateRequest(List.of(
                        new BusinessSettingValueRequest("PICKUP_TIME_UNDER_50_MINUTES", "0")
                ))
        )).isInstanceOf(ResponseStatusException.class).hasMessageContaining("positive whole number");

        assertThatThrownBy(() -> service.updateSystemSettings(
                AUTHENTICATION,
                new BusinessSettingsUpdateRequest(List.of(
                        new BusinessSettingValueRequest("PICKUP_TIME_UNDER_50_MINUTES", "15.5")
                ))
        )).isInstanceOf(ResponseStatusException.class).hasMessageContaining("whole number");
    }

    @Test
    void rejectsLocationUpdateWithoutPermission() {
        when(accountRoleService.resolveRoleNames(USER_ID)).thenReturn(List.of("ROLE_CUSTOMER"));

        assertThatThrownBy(() -> service.updateLocationSettings(
                AUTHENTICATION,
                LOCATION_ID,
                new BusinessSettingsUpdateRequest(List.of(
                        new BusinessSettingValueRequest("PICKUP_TIME_UNDER_50_MINUTES", "18")
                ))
        )).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }
}
