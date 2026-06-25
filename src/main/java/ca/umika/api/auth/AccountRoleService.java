package ca.umika.api.auth;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AccountRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    public AccountRoleService(UserRoleRepository userRoleRepository, RoleRepository roleRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
    }

    public String resolveRoleName(UUID userId) {
        List<UserRoleEntity> userRoles = userRoleRepository.findByIdUserId(userId);
        if (userRoles.isEmpty()) {
            return "ROLE_CUSTOMER";
        }
        UUID roleId = userRoles.get(0).getId().getRoleId();
        return roleRepository.findById(roleId)
                .map(RoleEntity::getName)
                .orElse("ROLE_CUSTOMER");
    }
}
