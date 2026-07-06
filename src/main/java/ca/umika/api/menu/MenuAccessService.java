package ca.umika.api.menu;

import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.store.LocationRepository;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MenuAccessService {

    public static final String MANAGE_PERMISSION_CODE = "MENU_MANAGE";
    private static final String LEGACY_MANAGE_PERMISSION_CODE = "LOCATION_SETTING_MANAGE";

    private final UserRepository userRepository;
    private final AccountRoleService accountRoleService;
    private final UserPermissionRepository userPermissionRepository;
    private final LocationRepository locationRepository;

    public MenuAccessService(
            UserRepository userRepository,
            AccountRoleService accountRoleService,
            UserPermissionRepository userPermissionRepository,
            LocationRepository locationRepository
    ) {
        this.userRepository = userRepository;
        this.accountRoleService = accountRoleService;
        this.userPermissionRepository = userPermissionRepository;
        this.locationRepository = locationRepository;
    }

    public MenuAccessContext assertReadAccess(Authentication authentication, UUID locationId) {
        UUID userId = resolveOptionalUserId(authentication);
        boolean admin = userId != null && accountRoleService.resolveRoleNames(userId).contains("ROLE_ADMIN");
        if (locationId != null) {
            ensureLocationExists(locationId);
        }
        return new MenuAccessContext(userId, admin, locationId);
    }

    public MenuAccessContext assertWriteAccess(Authentication authentication, UUID locationId) {
        UserEntity user = resolveUser(authentication);
        UUID userId = user.getId();
        List<String> roleNames = accountRoleService.resolveRoleNames(userId);
        boolean admin = roleNames.contains("ROLE_ADMIN");

        if (locationId == null) {
            if (admin || hasGlobalMenuPermission(userId)) {
                return new MenuAccessContext(userId, admin, null);
            }
            throw unauthorized();
        }

        ensureLocationExists(locationId);
        if (admin
                || hasGlobalMenuPermission(userId)
                || hasLocationMenuPermission(userId, locationId)
                || (isStoreRole(roleNames) && locationId.equals(user.getLocationId()))) {
            return new MenuAccessContext(userId, admin, locationId);
        }

        throw unauthorized();
    }

    private UUID resolveOptionalUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName())
                .map(UserEntity::getId)
                .orElse(null);
    }

    private UserEntity resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw unauthorized();
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    private boolean hasGlobalMenuPermission(UUID userId) {
        return hasGlobalPermission(userId, MANAGE_PERMISSION_CODE)
                || hasGlobalPermission(userId, LEGACY_MANAGE_PERMISSION_CODE);
    }

    private boolean hasLocationMenuPermission(UUID userId, UUID locationId) {
        return hasLocationPermission(userId, MANAGE_PERMISSION_CODE, locationId)
                || hasLocationPermission(userId, LEGACY_MANAGE_PERMISSION_CODE, locationId);
    }

    private boolean hasGlobalPermission(UUID userId, String permissionCode) {
        return userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(
                userId, permissionCode
        );
    }

    private boolean hasLocationPermission(UUID userId, String permissionCode, UUID locationId) {
        return userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(
                userId, permissionCode, locationId
        );
    }

    private boolean isStoreRole(List<String> roleNames) {
        return roleNames.contains("ROLE_MANAGER") || roleNames.contains("ROLE_STAFF");
    }

    private void ensureLocationExists(UUID locationId) {
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Location not found: " + locationId);
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing menu permission");
    }

    public record MenuAccessContext(UUID userId, boolean admin, UUID locationId) {
    }
}
