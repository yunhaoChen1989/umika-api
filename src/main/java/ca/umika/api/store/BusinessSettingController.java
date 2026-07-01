package ca.umika.api.store;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/business-settings", "/api/v1/manager/business-settings", "/api/manager/business-settings"})
@Tag(name = "BusinessSetting")
public class BusinessSettingController {

    private final BusinessSettingService service;

    public BusinessSettingController(BusinessSettingService service) {
        this.service = service;
    }

    @GetMapping("/effective")
    public BusinessSettingsResponse effective(
            Authentication authentication,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) String locationCode
    ) {
        return service.effective(authentication, locationId, locationCode);
    }

    @PutMapping("/location/{locationId}")
    public BusinessSettingsResponse updateLocationSettings(
            Authentication authentication,
            @PathVariable UUID locationId,
            @RequestBody BusinessSettingsUpdateRequest request
    ) {
        return service.updateLocationSettings(authentication, locationId, request);
    }

    @DeleteMapping("/location/{locationId}/{settingKey}")
    public BusinessSettingsResponse resetLocationSetting(
            Authentication authentication,
            @PathVariable UUID locationId,
            @PathVariable String settingKey
    ) {
        return service.resetLocationSetting(authentication, locationId, settingKey);
    }

    @PutMapping("/system")
    public BusinessSettingsResponse updateSystemSettings(
            Authentication authentication,
            @RequestBody BusinessSettingsUpdateRequest request
    ) {
        return service.updateSystemSettings(authentication, request);
    }
}
