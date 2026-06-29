package ca.umika.api.menu;

import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.store.LocationRepository;
import ca.umika.api.user.UserRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MenuAccessService {

    public static final String MANAGE_PERMISSION_CODE = "MENU_MANAGE";

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
        return assertAccess(authentication, locationId);
    }

    public MenuAccessContext assertWriteAccess(Authentication authentication, UUID locationId) {
        return assertAccess(authentication, locationId);
    }

    private MenuAccessContext assertAccess(Authentication authentication, UUID locationId) {
        UUID userId = resolveUserId(authentication);
        boolean admin = accountRoleService.resolveRoleNames(userId).contains("ROLE_ADMIN");

        if (locationId == null) {
            if (admin) {
                return new MenuAccessContext(userId, true, null);
            }
            throw unauthorized();
        }

        ensureLocationExists(locationId);
        if (admin || hasGlobalMenuPermission(userId) || hasLocationMenuPermission(userId, locationId)) {
            return new MenuAccessContext(userId, admin, locationId);
        }

        throw unauthorized();
    }

    private UUID resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw unauthorized();
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()))
                .getId();
    }

    private boolean hasGlobalMenuPermission(UUID userId) {
        return userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(
                userId, MANAGE_PERMISSION_CODE
        );
    }

    private boolean hasLocationMenuPermission(UUID userId, UUID locationId) {
        return userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(
                userId, MANAGE_PERMISSION_CODE, locationId
        );
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
