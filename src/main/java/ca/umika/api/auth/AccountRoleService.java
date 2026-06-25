package ca.umika.api.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
        List<UUID> roleIds = resolveRoleIds(userId);
        if (roleIds.isEmpty()) {
            return "ROLE_CUSTOMER";
        }
        UUID roleId = roleIds.get(0);
        return roleRepository.findById(roleId)
                .map(RoleEntity::getName)
                .orElse("ROLE_CUSTOMER");
    }

    public List<UUID> resolveRoleIds(UUID userId) {
        List<UUID> roleIds = userRoleRepository.findByIdUserId(userId).stream()
                .map(userRole -> userRole.getId().getRoleId())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        if (roleIds.isEmpty()) {
            roleRepository.findByName("ROLE_CUSTOMER").ifPresent(role -> roleIds.add(role.getId()));
        }
        return roleIds;
    }

    public List<String> resolveRoleNames(UUID userId) {
        return resolveRoleIds(userId).stream()
                .map(roleId -> roleRepository.findById(roleId).map(RoleEntity::getName).orElse("ROLE_CUSTOMER"))
                .distinct()
                .collect(Collectors.toList());
    }
}
