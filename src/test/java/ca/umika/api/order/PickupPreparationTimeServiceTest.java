package ca.umika.api.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.umika.api.admin.SystemSettingEntity;
import ca.umika.api.admin.SystemSettingRepository;
import ca.umika.api.store.BusinessHourEntity;
import ca.umika.api.store.BusinessHourRepository;
import ca.umika.api.store.LocationSettingEntity;
import ca.umika.api.store.LocationSettingRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.server.ResponseStatusException;

class PickupPreparationTimeServiceTest {

    private static final UUID LOCATION_ID = UUID.randomUUID();
    private static final ZoneId ZONE = ZoneId.of("America/Toronto");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-13T16:00:00Z"), ZONE);

    private SystemSettingRepository systemSettingRepository;
    private LocationSettingRepository locationSettingRepository;
    private BusinessHourRepository businessHourRepository;
    private PickupPreparationTimeService service;

    @BeforeEach
    void setUp() {
        systemSettingRepository = mock(SystemSettingRepository.class);
        locationSettingRepository = mock(LocationSettingRepository.class);
        businessHourRepository = mock(BusinessHourRepository.class);
        service = new PickupPreparationTimeService(
                systemSettingRepository,
                locationSettingRepository,
                businessHourRepository,
                CLOCK
        );
    }

    @ParameterizedTest
    @MethodSource("tierBoundaries")
    void selectsTierAtEveryBoundary(String finalTotal, int expectedMinutes) {
        assertThat(service.preparationMinutes(LOCATION_ID, new BigDecimal(finalTotal))).isEqualTo(expectedMinutes);
    }

    static Stream<Arguments> tierBoundaries() {
        return Stream.of(
                Arguments.of("49.99", 15),
                Arguments.of("50.00", 20),
                Arguments.of("50.01", 20),
                Arguments.of("79.99", 20),
                Arguments.of("80.00", 25),
                Arguments.of("80.01", 25),
                Arguments.of("99.99", 25),
                Arguments.of("100.00", 25),
                Arguments.of("100.01", 35)
        );
    }

    @Test
    void locationTierOverrideWinsOverSystemTier() {
        SystemSettingEntity system = systemSetting(PickupPreparationTimeService.FROM_50_TO_80, "21");
        LocationSettingEntity location = locationSetting(PickupPreparationTimeService.FROM_50_TO_80, "27");
        when(systemSettingRepository.findBySettingGroupAndSettingKeyIgnoreCase(
                "ORDER", PickupPreparationTimeService.FROM_50_TO_80)).thenReturn(Optional.of(system));
        when(locationSettingRepository.findByLocationIdAndSettingGroupIgnoreCaseAndSettingKeyIgnoreCase(
                LOCATION_ID, "ORDER", PickupPreparationTimeService.FROM_50_TO_80)).thenReturn(Optional.of(location));

        assertThat(service.preparationMinutes(LOCATION_ID, new BigDecimal("60"))).isEqualTo(27);
    }

    @Test
    void systemTierWinsBeforeDeprecatedLegacyFallback() {
        when(systemSettingRepository.findBySettingGroupAndSettingKeyIgnoreCase(
                "ORDER", PickupPreparationTimeService.UNDER_50)).thenReturn(Optional.of(systemSetting(PickupPreparationTimeService.UNDER_50, "18")));
        when(locationSettingRepository.findByLocationIdAndSettingGroupIgnoreCaseAndSettingKeyIgnoreCase(
                LOCATION_ID, "ORDER", "MIN_PICKUP_TIME_MINUTES")).thenReturn(Optional.of(locationSetting("MIN_PICKUP_TIME_MINUTES", "45")));

        assertThat(service.preparationMinutes(LOCATION_ID, new BigDecimal("20"))).isEqualTo(18);
    }

    @Test
    void usesDeprecatedLegacyValueOnlyWhenSelectedTierIsAbsent() {
        when(systemSettingRepository.findBySettingGroupAndSettingKeyIgnoreCase(
                "ORDER", "MIN_PICKUP_TIME_MINUTES"))
                .thenReturn(Optional.of(systemSetting("MIN_PICKUP_TIME_MINUTES", "30")));

        assertThat(service.preparationMinutes(LOCATION_ID, new BigDecimal("20"))).isEqualTo(30);
    }

    @Test
    void defaultsPickupTimeFromTrustedTotalTier() {
        LocalDateTime result = service.resolve(LOCATION_ID, "PICKUP", new BigDecimal("80.00"), null);

        assertThat(result).isEqualTo(LocalDateTime.now(CLOCK).plusMinutes(25));
    }

    @Test
    void rejectsRequestedPickupTimeBeforeTierMinimum() {
        LocalDateTime tooEarly = LocalDateTime.now(CLOCK).plusMinutes(19);

        assertThatThrownBy(() -> service.resolve(LOCATION_ID, "PICKUP", new BigDecimal("50.00"), tooEarly))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("at least 20 minutes");
    }

    @Test
    void doesNotApplyPickupRulesToOtherOrderTypes() {
        assertThat(service.resolve(LOCATION_ID, "DELIVERY", new BigDecimal("200"), null)).isNull();
        assertThat(service.resolve(LOCATION_ID, "DINE_IN", new BigDecimal("200"), null)).isNull();
    }

    @Test
    void preservesClosingCutoffValidation() {
        when(systemSettingRepository.findBySettingGroupAndSettingKeyIgnoreCase(
                "ORDER", "ORDER_CUTOFF_BEFORE_CLOSE_MINUTES"))
                .thenReturn(Optional.of(systemSetting("ORDER_CUTOFF_BEFORE_CLOSE_MINUTES", "20")));
        BusinessHourEntity hours = new BusinessHourEntity();
        hours.setLocationId(LOCATION_ID);
        hours.setDayOfWeek((short) 0);
        hours.setCloseTime(LocalTime.of(12, 30));
        hours.setIsClosed(false);
        when(businessHourRepository.findByLocationIdAndDayOfWeek(LOCATION_ID, (short) 0))
                .thenReturn(Optional.of(hours));

        assertThatThrownBy(() -> service.resolve(LOCATION_ID, "PICKUP", new BigDecimal("20"), null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("store is closing");
    }

    private SystemSettingEntity systemSetting(String key, String value) {
        SystemSettingEntity entity = new SystemSettingEntity();
        entity.setSettingGroup("ORDER");
        entity.setSettingKey(key);
        entity.setSettingValue(value);
        return entity;
    }

    private LocationSettingEntity locationSetting(String key, String value) {
        LocationSettingEntity entity = new LocationSettingEntity();
        entity.setLocationId(LOCATION_ID);
        entity.setSettingGroup("ORDER");
        entity.setSettingKey(key);
        entity.setSettingValue(value);
        return entity;
    }
}
