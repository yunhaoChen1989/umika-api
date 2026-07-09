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

    public List<ManagerMenuNodeDto> getMenus(Authentication authentication, String locale) {
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
        for (UUID menuId : directAllowedMenuIds) {
            includeWithAncestors(menuId, visibleMenuIds, menuMap);
        }

        if (visibleMenuIds.isEmpty()) {
            return List.of();
        }

        List<SystemMenuEntity> menus = allMenus.stream()
                .filter(menu -> visibleMenuIds.contains(menu.getId()))
                .filter(menu -> Boolean.TRUE.equals(menu.getIsEnabled()))
                .filter(menu -> Boolean.TRUE.equals(menu.getIsVisible()) || menu.getIsVisible() == null)
                .toList();

        return buildTree(menus, resolveLocale(locale));
    }

    private void includeWithAncestors(UUID menuId, Set<UUID> visibleMenuIds, Map<UUID, SystemMenuEntity> menuMap) {
        if (menuId == null || !visibleMenuIds.add(menuId)) {
            return;
        }

        SystemMenuEntity menu = menuMap.get(menuId);
        if (menu == null) {
            return;
        }

        includeWithAncestors(menu.getParentId(), visibleMenuIds, menuMap);
    }

    private List<ManagerMenuNodeDto> buildTree(List<SystemMenuEntity> menus, String locale) {
        Map<UUID, MenuNode> nodeMap = new LinkedHashMap<>();
        for (SystemMenuEntity menu : menus) {
            nodeMap.put(menu.getId(), new MenuNode(toDto(menu, locale)));
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
                null,
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

    private ManagerMenuNodeDto toDto(SystemMenuEntity menu, String locale) {
        return new ManagerMenuNodeDto(
                menu.getId(),
                menu.getParentId(),
                localizedName(menu, locale),
                localizedDescription(menu, locale),
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

    private String localizedName(SystemMenuEntity menu, String locale) {
        return switch (locale) {
            case "zh" -> firstPresent(menu.getNameZh(), menu.getNameEn(), menu.getName());
            case "ko" -> firstPresent(menu.getNameKo(), menu.getNameEn(), menu.getName());
            default -> firstPresent(menu.getNameEn(), menu.getName());
        };
    }

    private String localizedDescription(SystemMenuEntity menu, String locale) {
        return switch (locale) {
            case "zh" -> firstPresent(menu.getDescriptionZh(), menu.getDescriptionEn());
            case "ko" -> firstPresent(menu.getDescriptionKo(), menu.getDescriptionEn());
            default -> firstPresent(menu.getDescriptionEn());
        };
    }

    private String resolveLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return "en";
        }
        String normalized = locale.trim().toLowerCase();
        if (normalized.startsWith("zh")) {
            return "zh";
        }
        if (normalized.startsWith("ko")) {
            return "ko";
        }
        return "en";
    }

    private String firstPresent(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
                    menu.description(),
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
