package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MenuOptionAssignmentService {
    private final MenuItemRepository itemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuItemOptionRepository legacyOptionRepository;
    private final MenuOptionCategoryRepository categoryRepository;
    private final MenuOptionRepository optionRepository;
    private final MenuItemOptionCategoryAssignmentRepository assignmentRepository;
    private final MenuAccessService menuAccessService;

    public MenuOptionAssignmentService(MenuItemRepository itemRepository,
            MenuCategoryRepository menuCategoryRepository,
            MenuItemOptionRepository legacyOptionRepository,
            MenuOptionCategoryRepository categoryRepository,
            MenuOptionRepository optionRepository,
            MenuItemOptionCategoryAssignmentRepository assignmentRepository,
            MenuAccessService menuAccessService) {
        this.itemRepository = itemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.legacyOptionRepository = legacyOptionRepository;
        this.categoryRepository = categoryRepository;
        this.optionRepository = optionRepository;
        this.assignmentRepository = assignmentRepository;
        this.menuAccessService = menuAccessService;
    }

    @Transactional(readOnly = true)
    public List<MenuCatalogOptionGroupDto> getGroups(UUID itemId, UUID locationId) {
        ensureItemScope(itemId, locationId);
        return getGroupsForItems(List.of(itemId), locationId).getOrDefault(itemId, List.of());
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<MenuCatalogOptionGroupDto>> getGroupsForItems(List<UUID> itemIds, UUID locationId) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }
        List<MenuOptionCategoryEntity> categories = categoryRepository.findActiveAvailable(locationId);
        Map<UUID, MenuOptionCategoryEntity> categoryById = categories.stream()
                .collect(Collectors.toMap(MenuOptionCategoryEntity::getId, Function.identity()));
        Map<UUID, Map<UUID, Boolean>> assignmentsByItem = new HashMap<>();
        assignmentRepository.findByItemIdInAndLocationIdIsNull(itemIds).forEach(row -> assignmentsByItem
                .computeIfAbsent(row.getItemId(), ignored -> new HashMap<>())
                .put(row.getCategoryId(), Boolean.TRUE.equals(row.getIsEnabled())));
        if (locationId != null) {
            assignmentRepository.findByItemIdInAndLocationId(itemIds, locationId).forEach(row -> assignmentsByItem
                    .computeIfAbsent(row.getItemId(), ignored -> new HashMap<>())
                    .put(row.getCategoryId(), Boolean.TRUE.equals(row.getIsEnabled())));
        }
        Set<UUID> allEnabledCategoryIds = assignmentsByItem.values().stream()
                .flatMap(map -> map.entrySet().stream())
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .filter(categoryById::containsKey)
                .collect(Collectors.toSet());
        Map<UUID, List<MenuOptionEntity>> optionsByCategory = allEnabledCategoryIds.isEmpty()
                ? Map.of()
                : optionRepository.findByCategoryIdInAndIsActiveTrueOrderBySortOrderAsc(allEnabledCategoryIds).stream()
                        .collect(Collectors.groupingBy(MenuOptionEntity::getCategoryId));
        Map<UUID, List<MenuItemOptionEntity>> legacyByItem = legacyOptionRepository
                .findByItemIdInAndIsActiveTrueOrderBySortOrderAsc(itemIds).stream()
                .collect(Collectors.groupingBy(MenuItemOptionEntity::getItemId));

        Map<UUID, List<MenuCatalogOptionGroupDto>> result = new LinkedHashMap<>();
        for (UUID itemId : itemIds) {
            List<MenuCatalogOptionGroupDto> groups = new ArrayList<>();
            List<MenuItemOptionEntity> legacyOptions = legacyByItem.getOrDefault(itemId, List.of());
            if (!legacyOptions.isEmpty()) {
                groups.add(toLegacyGroup(legacyOptions));
            }
            assignmentsByItem.getOrDefault(itemId, Map.of()).entrySet().stream()
                    .filter(Map.Entry::getValue)
                    .map(Map.Entry::getKey)
                    .map(categoryById::get)
                    .filter(category -> category != null)
                    .sorted(Comparator.comparingInt(category -> valueOrZero(category.getSortOrder())))
                    .map(category -> toGroup(category, optionsByCategory.getOrDefault(category.getId(), List.of())))
                    .forEach(groups::add);
            result.put(itemId, groups);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<SelectedMenuOption> resolveSelections(UUID itemId, UUID locationId, List<UUID> selectedIds) {
        List<UUID> distinctIds = selectedIds == null ? List.of() : selectedIds.stream().distinct().toList();
        Map<UUID, MenuCatalogOptionDto> available = new LinkedHashMap<>();
        List<MenuCatalogOptionGroupDto> groups = getGroups(itemId, locationId);
        for (MenuCatalogOptionGroupDto group : groups) {
            if (group.options().isEmpty()) {
                continue;
            }
            Set<UUID> selectedInGroup = new HashSet<>();
            for (MenuCatalogOptionDto option : group.options()) {
                available.put(option.id(), option);
                if (distinctIds.contains(option.id())) {
                    selectedInGroup.add(option.id());
                }
            }
            int minimum = group.minSelect() == null ? (Boolean.TRUE.equals(group.isRequired()) ? 1 : 0) : group.minSelect();
            if (selectedInGroup.size() < minimum) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Select at least " + minimum + " option(s) from " + group.name());
            }
            if (group.maxSelect() != null && selectedInGroup.size() > group.maxSelect()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Select no more than " + group.maxSelect() + " option(s) from " + group.name());
            }
        }
        if (!available.keySet().containsAll(distinctIds)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "One or more selected options are unavailable for this menu item and location");
        }
        return distinctIds.stream().map(available::get)
                .map(option -> new SelectedMenuOption(option.id(), option.name(), defaultPrice(option.priceModifier())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MenuCatalogOptionGroupDto> getAssignments(Authentication authentication, UUID itemId, UUID locationId) {
        assertAssignmentAccess(authentication, itemId, locationId);
        return getGroups(itemId, locationId);
    }

    public List<MenuCatalogOptionGroupDto> replaceAssignments(Authentication authentication, UUID itemId,
            UUID locationId, MenuItemOptionAssignmentRequest request) {
        MenuItemEntity item = assertAssignmentAccess(authentication, itemId, locationId);
        Set<UUID> selected = request == null || request.categoryIds() == null
                ? Set.of() : new HashSet<>(request.categoryIds());
        List<MenuOptionCategoryEntity> candidates = categoryRepository.findActiveAvailable(locationId);
        Set<UUID> candidateIds = candidates.stream().map(MenuOptionCategoryEntity::getId).collect(Collectors.toSet());
        if (!candidateIds.containsAll(selected)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "One or more option categories are unavailable for this location");
        }

        if (locationId == null) {
            assignmentRepository.deleteByItemIdAndLocationIdIsNull(item.getId());
            assignmentRepository.saveAll(selected.stream().map(categoryId -> assignment(itemId, categoryId, null, true)).toList());
        } else {
            Set<UUID> inherited = assignmentRepository.findByItemIdAndLocationIdIsNull(itemId).stream()
                    .filter(row -> Boolean.TRUE.equals(row.getIsEnabled()))
                    .map(MenuItemOptionCategoryAssignmentEntity::getCategoryId)
                    .collect(Collectors.toSet());
            assignmentRepository.deleteByItemIdAndLocationId(itemId, locationId);
            List<MenuItemOptionCategoryAssignmentEntity> overrides = candidates.stream()
                    .filter(category -> selected.contains(category.getId()) != inherited.contains(category.getId()))
                    .map(category -> assignment(itemId, category.getId(), locationId, selected.contains(category.getId())))
                    .toList();
            assignmentRepository.saveAll(overrides);
        }
        return getGroups(itemId, locationId);
    }

    private MenuItemEntity assertAssignmentAccess(Authentication authentication, UUID itemId, UUID locationId) {
        MenuItemEntity item = getItem(itemId);
        UUID scope = locationId != null ? locationId : item.getLocationId();
        menuAccessService.assertWriteAccess(authentication, scope);
        ensureItemScope(item, locationId);
        return item;
    }

    private void ensureItemScope(UUID itemId, UUID locationId) {
        ensureItemScope(getItem(itemId), locationId);
    }

    private void ensureItemScope(MenuItemEntity item, UUID locationId) {
        if (item.getLocationId() != null && !item.getLocationId().equals(locationId)) {
            MenuCategoryEntity category = menuCategoryRepository.findById(item.getCategoryId()).orElse(null);
            if (category != null && category.getLocationId() == null) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Menu item does not belong to the requested location");
        }
    }

    private MenuItemEntity getItem(UUID itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemId));
    }

    private Set<UUID> resolveEnabledCategoryIds(UUID itemId, UUID locationId) {
        Map<UUID, Boolean> effective = new HashMap<>();
        assignmentRepository.findByItemIdAndLocationIdIsNull(itemId)
                .forEach(row -> effective.put(row.getCategoryId(), Boolean.TRUE.equals(row.getIsEnabled())));
        if (locationId != null) {
            assignmentRepository.findByItemIdAndLocationId(itemId, locationId)
                    .forEach(row -> effective.put(row.getCategoryId(), Boolean.TRUE.equals(row.getIsEnabled())));
        }
        return effective.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private MenuCatalogOptionGroupDto toGroup(MenuOptionCategoryEntity category, List<MenuOptionEntity> options) {
        return new MenuCatalogOptionGroupDto(category.getId(), category.getName(), category.getNameZh(),
                category.getNameKo(), category.getDescription(), category.getDescriptionZh(), category.getDescriptionKo(),
                category.getIsRequired(), category.getMinSelect(), category.getMaxSelect(), category.getSortOrder(),
                options.stream().sorted(Comparator.comparingInt(option -> valueOrZero(option.getSortOrder())))
                        .map(option -> new MenuCatalogOptionDto(option.getId(), option.getName(), option.getNameZh(),
                                option.getNameKo(), option.getDescription(), option.getDescriptionZh(),
                                option.getDescriptionKo(), defaultPrice(option.getPriceModifier()), option.getSortOrder(),
                                option.getIsActive())).toList());
    }

    private MenuCatalogOptionGroupDto toLegacyGroup(List<MenuItemOptionEntity> options) {
        boolean required = options.stream().anyMatch(option -> Boolean.TRUE.equals(option.getIsRequired()));
        return new MenuCatalogOptionGroupDto(null, "Options", null, null, null, null, null,
                required, required ? 1 : 0, null, -1,
                options.stream().map(option -> new MenuCatalogOptionDto(option.getId(), option.getName(),
                        null, null, null, null, null, defaultPrice(option.getPriceModifier()),
                        option.getSortOrder(), option.getIsActive())).toList());
    }

    private MenuItemOptionCategoryAssignmentEntity assignment(UUID itemId, UUID categoryId, UUID locationId,
            boolean enabled) {
        MenuItemOptionCategoryAssignmentEntity entity = new MenuItemOptionCategoryAssignmentEntity();
        entity.setItemId(itemId);
        entity.setCategoryId(categoryId);
        entity.setLocationId(locationId);
        entity.setIsEnabled(enabled);
        return entity;
    }

    private BigDecimal defaultPrice(BigDecimal value) { return value == null ? BigDecimal.ZERO : value; }
    private int valueOrZero(Integer value) { return value == null ? 0 : value; }

    public record SelectedMenuOption(UUID id, String name, BigDecimal priceModifier) {
    }
}
