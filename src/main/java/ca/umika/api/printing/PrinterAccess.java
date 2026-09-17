package ca.umika.api.printing;

import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.user.UserRepository;
import ca.umika.api.store.LocationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

@Service
public class PrinterAccess {
    private final UserRepository users;
    private final AccountRoleService roles;
    private final UserPermissionRepository permissions;
    private final LocationRepository locations;
    public PrinterAccess(UserRepository users, AccountRoleService roles, UserPermissionRepository permissions, LocationRepository locations) {
        this.users=users; this.roles=roles; this.permissions=permissions; this.locations=locations;
    }
    public void require(Authentication auth, UUID locationId) {
        requirePermission(auth,locationId,"LOCATION_SETTING_MANAGE");
    }
    public void requireOrder(Authentication auth, UUID locationId) {
        requirePermission(auth,locationId,"ORDER_MANAGE");
    }
    private void requirePermission(Authentication auth, UUID locationId, String permission) {
        if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        var user=users.findByEmail(auth.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!locations.existsById(locationId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (roles.resolveRoleNames(user.getId()).contains("ROLE_ADMIN")) return;
        boolean allowed=permissions.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(user.getId(), permission)
            || permissions.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(user.getId(), permission, locationId);
        if (!allowed) throw new ResponseStatusException(HttpStatus.FORBIDDEN, permission);
    }
}
