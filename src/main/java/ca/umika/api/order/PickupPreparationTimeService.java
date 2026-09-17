package ca.umika.api.order;

import ca.umika.api.admin.SystemSettingRepository;
import ca.umika.api.store.BusinessHourEntity;
import ca.umika.api.store.BusinessHourRepository;
import ca.umika.api.store.LocationSettingRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class PickupPreparationTimeService {

    static final String UNDER_50 = "PICKUP_TIME_UNDER_50_MINUTES";
    static final String FROM_50_TO_80 = "PICKUP_TIME_50_TO_80_MINUTES";
    static final String FROM_80_TO_100 = "PICKUP_TIME_80_TO_100_MINUTES";
    static final String OVER_100 = "PICKUP_TIME_OVER_100_MINUTES";
    private static final String LEGACY_MINIMUM = "MIN_PICKUP_TIME_MINUTES";
    private static final String CLOSING_CUTOFF = "ORDER_CUTOFF_BEFORE_CLOSE_MINUTES";
    private static final String GROUP = "ORDER";

    private final SystemSettingRepository systemSettingRepository;
    private final LocationSettingRepository locationSettingRepository;
    private final BusinessHourRepository businessHourRepository;
    private final Clock clock;

    public PickupPreparationTimeService(
            SystemSettingRepository systemSettingRepository,
            LocationSettingRepository locationSettingRepository,
            BusinessHourRepository businessHourRepository
    ) {
        this(systemSettingRepository, locationSettingRepository, businessHourRepository, Clock.systemDefaultZone());
    }

    PickupPreparationTimeService(
            SystemSettingRepository systemSettingRepository,
            LocationSettingRepository locationSettingRepository,
            BusinessHourRepository businessHourRepository,
            Clock clock
    ) {
        this.systemSettingRepository = systemSettingRepository;
        this.locationSettingRepository = locationSettingRepository;
        this.businessHourRepository = businessHourRepository;
        this.clock = clock;
    }

    public LocalDateTime resolve(
            UUID locationId,
            String orderType,
            BigDecimal finalTotal,
            LocalDateTime requestedPickupTime
    ) {
        return resolve(locationId, orderType, finalTotal, requestedPickupTime, true);
    }

    public LocalDateTime resolve(
            UUID locationId,
            String orderType,
            BigDecimal finalTotal,
            LocalDateTime requestedPickupTime,
            boolean autoAcceptOrders
    ) {
        if (!"PICKUP".equals(orderType)) {
            return null;
        }
        if (!autoAcceptOrders) {
            LocalDateTime now = LocalDateTime.now(clock);
            if (requestedPickupTime != null && requestedPickupTime.isBefore(now)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requestedPickupTime cannot be in the past");
            }
            validateBeforeClosingCutoff(locationId, requestedPickupTime == null ? now : requestedPickupTime);
            return requestedPickupTime;
        }
        if (finalTotal == null || finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Backend-calculated finalTotal is required for pickup preparation time");
        }

        int preparationMinutes = preparationMinutes(locationId, finalTotal);
        LocalDateTime earliestPickupTime = LocalDateTime.now(clock).plusMinutes(preparationMinutes);
        LocalDateTime pickupTime = requestedPickupTime == null ? earliestPickupTime : requestedPickupTime;
        if (pickupTime.isBefore(earliestPickupTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "requestedPickupTime must be at least " + preparationMinutes + " minutes from now"
            );
        }
        validateBeforeClosingCutoff(locationId, pickupTime);
        return pickupTime;
    }

    int preparationMinutes(UUID locationId, BigDecimal finalTotal) {
        Tier tier = tierFor(finalTotal);
        String value = settingValue(locationId, tier.key())
                .or(() -> settingValue(locationId, LEGACY_MINIMUM))
                .orElse(Integer.toString(tier.defaultMinutes()));
        try {
            BigDecimal minutes = new BigDecimal(value.trim());
            if (minutes.compareTo(BigDecimal.ZERO) <= 0 || minutes.stripTrailingZeros().scale() > 0) {
                throw new NumberFormatException();
            }
            return minutes.intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid positive whole-minute setting value for " + tier.key());
        }
    }

    private Tier tierFor(BigDecimal finalTotal) {
        if (finalTotal.compareTo(BigDecimal.valueOf(50)) < 0) {
            return new Tier(UNDER_50, 15);
        }
        if (finalTotal.compareTo(BigDecimal.valueOf(80)) < 0) {
            return new Tier(FROM_50_TO_80, 20);
        }
        if (finalTotal.compareTo(BigDecimal.valueOf(100)) <= 0) {
            return new Tier(FROM_80_TO_100, 25);
        }
        return new Tier(OVER_100, 35);
    }

    private Optional<String> settingValue(UUID locationId, String key) {
        Optional<String> locationValue = locationId == null ? Optional.empty() : locationSettingRepository
                .findByLocationIdAndSettingGroupIgnoreCaseAndSettingKeyIgnoreCase(locationId, GROUP, key)
                .map(setting -> setting.getSettingValue())
                .filter(value -> value != null && !value.isBlank())
                .or(() -> locationSettingRepository.findByLocationIdAndSettingKeyIgnoreCase(locationId, key)
                        .map(setting -> setting.getSettingValue())
                        .filter(value -> value != null && !value.isBlank()));
        return locationValue.or(() -> systemSettingRepository
                .findBySettingGroupAndSettingKeyIgnoreCase(GROUP, key)
                .map(setting -> setting.getSettingValue())
                .filter(value -> value != null && !value.isBlank())
                .or(() -> systemSettingRepository.findBySettingKey(key)
                        .map(setting -> setting.getSettingValue())
                        .filter(value -> value != null && !value.isBlank())));
    }

    private void validateBeforeClosingCutoff(UUID locationId, LocalDateTime pickupTime) {
        int cutoffMinutes = nonNegativeWholeMinutes(settingValue(locationId, CLOSING_CUTOFF).orElse("0"), CLOSING_CUTOFF);
        if (cutoffMinutes <= 0 || businessHourRepository == null || !pickupTime.toLocalDate().isEqual(LocalDate.now(clock))) {
            return;
        }
        short dayOfWeek = (short) (pickupTime.getDayOfWeek().getValue() % 7);
        BusinessHourEntity hours = businessHourRepository.findByLocationIdAndDayOfWeek(locationId, dayOfWeek).orElse(null);
        if (hours == null) {
            return;
        }
        if (Boolean.TRUE.equals(hours.getIsClosed()) || hours.getCloseTime() == null) {
            throw closingException();
        }
        LocalDateTime closingCutoff = LocalDateTime.of(pickupTime.toLocalDate(), hours.getCloseTime()).minusMinutes(cutoffMinutes);
        if (!pickupTime.isBefore(closingCutoff)) {
            throw closingException();
        }
    }

    private int nonNegativeWholeMinutes(String value, String key) {
        try {
            BigDecimal minutes = new BigDecimal(value.trim());
            if (minutes.compareTo(BigDecimal.ZERO) < 0 || minutes.stripTrailingZeros().scale() > 0) {
                throw new NumberFormatException();
            }
            return minutes.intValueExact();
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid setting value for " + key);
        }
    }

    private ResponseStatusException closingException() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The store is closing, you can only order for tomorrow"
        );
    }

    private record Tier(String key, int defaultMinutes) {
    }
}
