package ca.umika.api.admin;

import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.user.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional(readOnly = true)
public class ManagerMenuService {

    private final UserRepository userRepository;
    private final AccountRoleService accountRoleService;
    private final RoleMenuRepository roleMenuRepository;
    private final SystemMenuRepository systemMenuRepository;

    public ManagerMenuService(
            UserRepository userRepository,
            AccountRoleService accountRoleService,
            RoleMenuRepository roleMenuRepository,
            SystemMenuRepository systemMenuRepository
    ) {
        this.userRepository = userRepository;
        this.accountRoleService = accountRoleService;
        this.roleMenuRepository = roleMenuRepository;
        this.systemMenuRepository = systemMenuRepository;
    }

    public List<ManagerMenuNodeDto> getMenus(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        UUID userId = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()))
                .getId();

        List<UUID> roleIds = accountRoleService.resolveRoleIds(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> directAllowedMenuIds = roleMenuRepository.findByIdRoleIdIn(roleIds).stream()
                .map(roleMenu -> roleMenu.getId().getMenuId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (directAllowedMenuIds.isEmpty()) {
            return List.of();
        }

        List<SystemMenuEntity> allMenus = systemMenuRepository.findAll(
                Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("name"))
        );
        Map<UUID, SystemMenuEntity> menuMap = allMenus.stream()
                .collect(Collectors.toMap(
                        SystemMenuEntity::getId,
                        menu -> menu,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Set<UUID> visibleMenuIds = new LinkedHashSet<>();
        for (SystemMenuEntity menu : allMenus) {
            if (isVisibleToRole(menu, directAllowedMenuIds, menuMap)) {
                visibleMenuIds.add(menu.getId());
            }
        }

        if (visibleMenuIds.isEmpty()) {
            return List.of();
        }

        List<SystemMenuEntity> menus = allMenus.stream()
                .filter(menu -> visibleMenuIds.contains(menu.getId()))
                .filter(menu -> Boolean.TRUE.equals(menu.getIsEnabled()))
                .filter(menu -> Boolean.TRUE.equals(menu.getIsVisible()) || menu.getIsVisible() == null)
                .toList();

        return buildTree(menus);
    }

    private boolean isVisibleToRole(
            SystemMenuEntity menu,
            Set<UUID> directAllowedMenuIds,
            Map<UUID, SystemMenuEntity> menuMap
    ) {
        if (directAllowedMenuIds.contains(menu.getId())) {
            return true;
        }

        UUID parentId = menu.getParentId();
        while (parentId != null) {
            if (directAllowedMenuIds.contains(parentId)) {
                return true;
            }
            SystemMenuEntity parent = menuMap.get(parentId);
            if (parent == null) {
                break;
            }
            parentId = parent.getParentId();
        }
        return false;
    }

    private List<ManagerMenuNodeDto> buildTree(List<SystemMenuEntity> menus) {
        Map<UUID, MenuNode> nodeMap = new LinkedHashMap<>();
        for (SystemMenuEntity menu : menus) {
            nodeMap.put(menu.getId(), new MenuNode(toDto(menu)));
        }

        List<MenuNode> roots = new ArrayList<>();
        for (SystemMenuEntity menu : menus) {
            MenuNode node = nodeMap.get(menu.getId());
            if (menu.getParentId() != null && nodeMap.containsKey(menu.getParentId())) {
                nodeMap.get(menu.getParentId()).children.add(node);
            } else {
                roots.add(node);
            }
        }

        return roots.stream()
                .sorted(Comparator.comparing((MenuNode node) -> node.menu.sortOrder(), Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(node -> node.menu.name(), Comparator.nullsLast(String::compareTo)))
                .map(MenuNode::toDto)
                .toList();
    }

    private ManagerMenuNodeDto toDto(SystemMenuEntity menu) {
        return new ManagerMenuNodeDto(
                menu.getId(),
                menu.getParentId(),
                menu.getName(),
                menu.getCode(),
                menu.getPath(),
                menu.getComponent(),
                menu.getIcon(),
                menu.getMenuType(),
                menu.getSortOrder(),
                menu.getIsVisible(),
                menu.getIsEnabled(),
                List.of()
        );
    }

    private static final class MenuNode {
        private final ManagerMenuNodeDto menu;
        private final List<MenuNode> children = new ArrayList<>();

        private MenuNode(ManagerMenuNodeDto menu) {
            this.menu = menu;
        }

        private ManagerMenuNodeDto toDto() {
            return new ManagerMenuNodeDto(
                    menu.id(),
                    menu.parentId(),
                    menu.name(),
                    menu.code(),
                    menu.path(),
                    menu.component(),
                    menu.icon(),
                    menu.menuType(),
                    menu.sortOrder(),
                    menu.isVisible(),
                    menu.isEnabled(),
                    children.stream()
                            .sorted(Comparator.comparing((MenuNode node) -> node.menu.sortOrder(), Comparator.nullsLast(Integer::compareTo))
                                    .thenComparing(node -> node.menu.name(), Comparator.nullsLast(String::compareTo)))
                            .map(MenuNode::toDto)
                            .toList()
            );
        }
    }
}
