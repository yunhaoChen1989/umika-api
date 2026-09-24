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
    private final MenuOptionCategoryRepository optionCategoryRepository;
    private final MenuOptionRepository optionRepository;
    private final MenuItemOptionCategoryAssignmentRepository assignmentRepository;
    private final MenuItemOptionAssignmentSettingRepository itemSettingRepository;
    private final MenuAccessService menuAccessService;

    public MenuOptionAssignmentService(MenuItemRepository itemRepository,
            MenuCategoryRepository menuCategoryRepository,
            MenuOptionCategoryRepository optionCategoryRepository,
            MenuOptionRepository optionRepository,
            MenuItemOptionCategoryAssignmentRepository assignmentRepository,
            MenuItemOptionAssignmentSettingRepository itemSettingRepository,
            MenuAccessService menuAccessService) {
        this.itemRepository = itemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.optionCategoryRepository = optionCategoryRepository;
        this.optionRepository = optionRepository;
        this.assignmentRepository = assignmentRepository;
        this.itemSettingRepository = itemSettingRepository;
        this.menuAccessService = menuAccessService;
    }

    @Transactional(readOnly = true)
    public List<MenuCatalogOptionGroupDto> getGroups(UUID itemId, UUID locationId) {
        MenuItemEntity item = getItem(itemId);
        ensureItemScope(item, locationId);
        return getGroupsForItems(List.of(item), locationId).getOrDefault(itemId, List.of());
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<MenuCatalogOptionGroupDto>> getGroupsForItems(List<MenuItemEntity> items, UUID locationId) {
        if (items.isEmpty()) {
            return Map.of();
        }
        List<UUID> itemIds = items.stream().map(MenuItemEntity::getId).toList();
        List<UUID> menuCategoryIds = items.stream().map(MenuItemEntity::getCategoryId).distinct().toList();
        Map<UUID, MenuOptionCategoryEntity> availableCategories = optionCategoryRepository.findActiveAvailable(locationId)
                .stream().collect(Collectors.toMap(MenuOptionCategoryEntity::getId, Function.identity()));

        Map<UUID, Map<UUID, Boolean>> categoryAssignments = new HashMap<>();
        assignmentRepository.findByMenuCategoryIdInAndLocationIdIsNull(menuCategoryIds).forEach(row -> categoryAssignments
                .computeIfAbsent(row.getMenuCategoryId(), ignored -> new HashMap<>())
                .put(row.getCategoryId(), Boolean.TRUE.equals(row.getIsEnabled())));
        if (locationId != null) {
            assignmentRepository.findByMenuCategoryIdInAndLocationId(menuCategoryIds, locationId).forEach(row -> categoryAssignments
                    .computeIfAbsent(row.getMenuCategoryId(), ignored -> new HashMap<>())
                    .put(row.getCategoryId(), Boolean.TRUE.equals(row.getIsEnabled())));
        }

        Map<UUID, Map<UUID, Boolean>> itemAssignments = new HashMap<>();
        assignmentRepository.findByItemIdInAndLocationIdIsNull(itemIds).forEach(row -> itemAssignments
                .computeIfAbsent(row.getItemId(), ignored -> new HashMap<>())
                .put(row.getCategoryId(), Boolean.TRUE.equals(row.getIsEnabled())));
        if (locationId != null) {
            assignmentRepository.findByItemIdInAndLocationId(itemIds, locationId).forEach(row -> itemAssignments
                    .computeIfAbsent(row.getItemId(), ignored -> new HashMap<>())
                    .put(row.getCategoryId(), Boolean.TRUE.equals(row.getIsEnabled())));
        }

        Map<UUID, MenuItemOptionAssignmentSettingEntity> globalItemSettings = itemSettingRepository
                .findByItemIdInAndLocationIdIsNull(itemIds).stream()
                .collect(Collectors.toMap(MenuItemOptionAssignmentSettingEntity::getItemId, Function.identity()));
        Map<UUID, MenuItemOptionAssignmentSettingEntity> locationItemSettings = locationId == null ? Map.of()
                : itemSettingRepository.findByItemIdInAndLocationId(itemIds, locationId).stream()
                        .collect(Collectors.toMap(MenuItemOptionAssignmentSettingEntity::getItemId, Function.identity()));

        Set<UUID> effectiveOptionCategoryIds = new HashSet<>();
        Map<UUID, Set<UUID>> categoryIdsByItem = new HashMap<>();
        for (MenuItemEntity item : items) {
            MenuItemOptionAssignmentSettingEntity itemSetting = locationItemSettings.getOrDefault(
                    item.getId(), globalItemSettings.get(item.getId()));
            Map<UUID, Boolean> source = Boolean.TRUE.equals(itemSetting == null ? null : itemSetting.getIsCustomized())
                    ? itemAssignments.getOrDefault(item.getId(), Map.of())
                    : categoryAssignments.getOrDefault(item.getCategoryId(), Map.of());
            Set<UUID> ids = source.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey)
                    .filter(availableCategories::containsKey).collect(Collectors.toCollection(HashSet::new));
            categoryIdsByItem.put(item.getId(), ids);
            effectiveOptionCategoryIds.addAll(ids);
        }

        Map<UUID, List<MenuOptionEntity>> optionsByCategory = effectiveOptionCategoryIds.isEmpty() ? Map.of()
                : optionRepository.findByCategoryIdInAndIsActiveTrueOrderBySortOrderAsc(effectiveOptionCategoryIds).stream()
                        .collect(Collectors.groupingBy(MenuOptionEntity::getCategoryId));
        Map<UUID, List<MenuCatalogOptionGroupDto>> result = new LinkedHashMap<>();
        for (MenuItemEntity item : items) {
            List<MenuCatalogOptionGroupDto> groups = new ArrayList<>();
            categoryIdsByItem.getOrDefault(item.getId(), Set.of()).stream()
                    .map(availableCategories::get)
                    .filter(category -> category != null)
                    .sorted(Comparator.comparingInt(category -> valueOrZero(category.getSortOrder())))
                    .map(category -> toGroup(category, optionsByCategory.getOrDefault(category.getId(), List.of())))
                    .forEach(groups::add);
            result.put(item.getId(), groups);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<SelectedMenuOption> resolveSelections(UUID itemId, UUID locationId, List<UUID> selectedIds) {
        List<UUID> distinctIds = selectedIds == null ? List.of() : selectedIds.stream().distinct().toList();
        Map<UUID, MenuCatalogOptionDto> available = new LinkedHashMap<>();
        for (MenuCatalogOptionGroupDto group : getGroups(itemId, locationId)) {
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
    public MenuOptionAssignmentResponse getItemAssignments(Authentication authentication, UUID itemId, UUID locationId) {
        MenuItemEntity item = assertItemAssignmentAccess(authentication, itemId, locationId);
        boolean customized = resolveItemCustomized(itemId, locationId);
        Set<UUID> direct = enabledIds(resolveItemAssignmentRows(itemId, locationId));
        Set<UUID> effective = customized ? direct : getCategoryAssignmentIds(item.getCategoryId(), locationId);
        return new MenuOptionAssignmentResponse(customized, direct, effective);
    }

    public MenuOptionAssignmentResponse replaceItemAssignments(Authentication authentication, UUID itemId,
            UUID locationId, MenuItemOptionAssignmentRequest request) {
        MenuItemEntity item = assertItemAssignmentAccess(authentication, itemId, locationId);
        boolean customized = request == null || request.customized() == null || Boolean.TRUE.equals(request.customized());
        Set<UUID> selected = request == null || request.categoryIds() == null ? Set.of() : new HashSet<>(request.categoryIds());
        validateSelectedOptionCategories(selected, locationId);

        saveItemSetting(itemId, locationId, customized);
        if (customized) {
            replaceAssignments(itemId, null, locationId, selected,
                    locationId == null ? Set.of() : enabledIds(assignmentRepository.findByItemIdAndLocationIdIsNull(itemId)));
        } else if (locationId == null) {
            assignmentRepository.deleteByItemIdAndLocationIdIsNull(itemId);
        } else {
            assignmentRepository.deleteByItemIdAndLocationId(itemId, locationId);
        }
        Set<UUID> direct = enabledIds(resolveItemAssignmentRows(itemId, locationId));
        Set<UUID> effective = customized ? direct : getCategoryAssignmentIds(item.getCategoryId(), locationId);
        return new MenuOptionAssignmentResponse(customized, direct, effective);
    }

    @Transactional(readOnly = true)
    public MenuOptionAssignmentResponse getMenuCategoryAssignments(Authentication authentication, UUID menuCategoryId,
            UUID locationId) {
        MenuCategoryEntity menuCategory = assertMenuCategoryAssignmentAccess(authentication, menuCategoryId, locationId);
        return new MenuOptionAssignmentResponse(true, getCategoryAssignmentIds(menuCategory.getId(), locationId),
                getCategoryAssignmentIds(menuCategory.getId(), locationId));
    }

    public MenuOptionAssignmentResponse replaceMenuCategoryAssignments(Authentication authentication, UUID menuCategoryId,
            UUID locationId, MenuItemOptionAssignmentRequest request) {
        MenuCategoryEntity menuCategory = assertMenuCategoryAssignmentAccess(authentication, menuCategoryId, locationId);
        Set<UUID> selected = request == null || request.categoryIds() == null ? Set.of() : new HashSet<>(request.categoryIds());
        validateSelectedOptionCategories(selected, locationId);
        Set<UUID> inherited = locationId == null ? Set.of()
                : enabledIds(assignmentRepository.findByMenuCategoryIdAndLocationIdIsNull(menuCategory.getId()));
        replaceAssignments(null, menuCategory.getId(), locationId, selected, inherited);
        Set<UUID> effective = getCategoryAssignmentIds(menuCategory.getId(), locationId);
        return new MenuOptionAssignmentResponse(true, effective, effective);
    }

    private void replaceAssignments(UUID itemId, UUID menuCategoryId, UUID locationId, Set<UUID> selected,
            Set<UUID> inherited) {
        if (itemId != null) {
            if (locationId == null) assignmentRepository.deleteByItemIdAndLocationIdIsNull(itemId);
            else assignmentRepository.deleteByItemIdAndLocationId(itemId, locationId);
        } else {
            if (locationId == null) assignmentRepository.deleteByMenuCategoryIdAndLocationIdIsNull(menuCategoryId);
            else assignmentRepository.deleteByMenuCategoryIdAndLocationId(menuCategoryId, locationId);
        }
        Set<UUID> toPersist = locationId == null ? selected : union(selected, inherited);
        List<MenuItemOptionCategoryAssignmentEntity> rows = toPersist.stream()
                .filter(categoryId -> locationId == null || selected.contains(categoryId) != inherited.contains(categoryId))
                .map(categoryId -> assignment(itemId, menuCategoryId, categoryId, locationId, selected.contains(categoryId)))
                .toList();
        assignmentRepository.saveAll(rows);
    }

    private MenuItemEntity assertItemAssignmentAccess(Authentication authentication, UUID itemId, UUID locationId) {
        MenuItemEntity item = getItem(itemId);
        UUID permissionLocationId = locationId != null ? locationId : item.getLocationId();
        menuAccessService.assertWriteAccess(authentication, permissionLocationId);
        ensureItemScope(item, locationId);
        return item;
    }

    private MenuCategoryEntity assertMenuCategoryAssignmentAccess(Authentication authentication, UUID menuCategoryId,
            UUID locationId) {
        MenuCategoryEntity menuCategory = menuCategoryRepository.findById(menuCategoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu category not found: " + menuCategoryId));
        UUID permissionLocationId = locationId != null ? locationId : menuCategory.getLocationId();
        menuAccessService.assertWriteAccess(authentication, permissionLocationId);
        if (menuCategory.getLocationId() != null && !menuCategory.getLocationId().equals(locationId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu category does not belong to the requested location");
        }
        return menuCategory;
    }

    private void validateSelectedOptionCategories(Set<UUID> selected, UUID locationId) {
        Set<UUID> availableIds = optionCategoryRepository.findActiveAvailable(locationId).stream()
                .map(MenuOptionCategoryEntity::getId).collect(Collectors.toSet());
        if (!availableIds.containsAll(selected)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "One or more add-on categories are unavailable for this location");
        }
    }

    private boolean resolveItemCustomized(UUID itemId, UUID locationId) {
        if (locationId != null) {
            MenuItemOptionAssignmentSettingEntity locationSetting = itemSettingRepository
                    .findByItemIdAndLocationId(itemId, locationId).orElse(null);
            if (locationSetting != null) return Boolean.TRUE.equals(locationSetting.getIsCustomized());
        }
        return itemSettingRepository.findByItemIdAndLocationIdIsNull(itemId)
                .map(MenuItemOptionAssignmentSettingEntity::getIsCustomized).orElse(false);
    }

    private void saveItemSetting(UUID itemId, UUID locationId, boolean customized) {
        MenuItemOptionAssignmentSettingEntity entity = (locationId == null
                ? itemSettingRepository.findByItemIdAndLocationIdIsNull(itemId)
                : itemSettingRepository.findByItemIdAndLocationId(itemId, locationId)).orElseGet(() -> {
                    MenuItemOptionAssignmentSettingEntity created = new MenuItemOptionAssignmentSettingEntity();
                    created.setItemId(itemId);
                    created.setLocationId(locationId);
                    return created;
                });
        entity.setIsCustomized(customized);
        itemSettingRepository.save(entity);
    }

    private List<MenuItemOptionCategoryAssignmentEntity> resolveItemAssignmentRows(UUID itemId, UUID locationId) {
        Map<UUID, MenuItemOptionCategoryAssignmentEntity> effective = new LinkedHashMap<>();
        assignmentRepository.findByItemIdAndLocationIdIsNull(itemId)
                .forEach(row -> effective.put(row.getCategoryId(), row));
        if (locationId != null) {
            assignmentRepository.findByItemIdAndLocationId(itemId, locationId)
                    .forEach(row -> effective.put(row.getCategoryId(), row));
        }
        return new ArrayList<>(effective.values());
    }

    private Set<UUID> getCategoryAssignmentIds(UUID menuCategoryId, UUID locationId) {
        Map<UUID, Boolean> effective = new LinkedHashMap<>();
        assignmentRepository.findByMenuCategoryIdAndLocationIdIsNull(menuCategoryId)
                .forEach(row -> effective.put(row.getCategoryId(), Boolean.TRUE.equals(row.getIsEnabled())));
        if (locationId != null) {
            assignmentRepository.findByMenuCategoryIdAndLocationId(menuCategoryId, locationId)
                    .forEach(row -> effective.put(row.getCategoryId(), Boolean.TRUE.equals(row.getIsEnabled())));
        }
        return enabledIds(effective);
    }

    private Set<UUID> enabledIds(List<MenuItemOptionCategoryAssignmentEntity> rows) {
        return rows.stream().filter(row -> Boolean.TRUE.equals(row.getIsEnabled()))
                .map(MenuItemOptionCategoryAssignmentEntity::getCategoryId).collect(Collectors.toCollection(HashSet::new));
    }

    private Set<UUID> enabledIds(Map<UUID, Boolean> assignments) {
        return assignments.entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Set<UUID> union(Set<UUID> left, Set<UUID> right) {
        Set<UUID> result = new HashSet<>(left);
        result.addAll(right);
        return result;
    }

    private void ensureItemScope(MenuItemEntity item, UUID locationId) {
        if (item.getLocationId() != null && !item.getLocationId().equals(locationId)) {
            MenuCategoryEntity category = menuCategoryRepository.findById(item.getCategoryId()).orElse(null);
            if (category != null && category.getLocationId() == null) return;
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Menu item does not belong to the requested location");
        }
    }

    private MenuItemEntity getItem(UUID itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemId));
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

    private MenuItemOptionCategoryAssignmentEntity assignment(UUID itemId, UUID menuCategoryId, UUID categoryId,
            UUID locationId, boolean enabled) {
        MenuItemOptionCategoryAssignmentEntity entity = new MenuItemOptionCategoryAssignmentEntity();
        entity.setItemId(itemId);
        entity.setMenuCategoryId(menuCategoryId);
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
