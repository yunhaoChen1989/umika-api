package ca.umika.api.printing;

import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.store.LocationRepository;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrinterAccessTest {
    @Test void locationPermissionDoesNotGrantOtherLocations() {
        var users=mock(UserRepository.class); var roles=mock(AccountRoleService.class);
        var permissions=mock(UserPermissionRepository.class); var locations=mock(LocationRepository.class);
        var user=mock(UserEntity.class); UUID userId=UUID.randomUUID(),location=UUID.randomUUID(),other=UUID.randomUUID();
        when(user.getId()).thenReturn(userId); when(users.findByEmail("manager@example.test")).thenReturn(Optional.of(user));
        when(locations.existsById(any())).thenReturn(true); when(roles.resolveRoleNames(userId)).thenReturn(List.of("ROLE_MANAGER"));
        when(permissions.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(userId,"LOCATION_SETTING_MANAGE",location)).thenReturn(true);
        var access=new PrinterAccess(users,roles,permissions,locations);
        var auth=new UsernamePasswordAuthenticationToken("manager@example.test","",List.of());
        assertDoesNotThrow(()->access.require(auth,location));
        assertEquals(403,assertThrows(ResponseStatusException.class,()->access.require(auth,other)).getStatusCode().value());
        assertEquals(401,assertThrows(ResponseStatusException.class,()->access.require(null,location)).getStatusCode().value());
        when(permissions.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(userId,"LOCATION_SETTING_MANAGE")).thenReturn(true);
        assertDoesNotThrow(()->access.require(auth,other));
    }
}
