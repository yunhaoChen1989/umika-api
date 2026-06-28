package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.store.LocationEntity;
import ca.umika.api.store.LocationRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MenuCatalogService {

    private final LocationRepository locationRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final LocationMenuOverrideRepository overrideRepository;

    public MenuCatalogService(
            LocationRepository locationRepository,
            MenuCategoryRepository categoryRepository,
            MenuItemRepository itemRepository,
            LocationMenuOverrideRepository overrideRepository
    ) {
        this.locationRepository = locationRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.overrideRepository = overrideRepository;
    }

    public MenuCatalogResponseDto resolve(String locationCode) {
        UUID locationId = resolveLocationId(locationCode);

        List<MenuCategoryEntity> categories = new ArrayList<>(categoryRepository.findAllByIsDeletedFalseAndLocationIdIsNullOrderBySortOrderAscCreatedAtAsc());
        if (locationId != null) {
            categories.addAll(categoryRepository.findAllByIsDeletedFalseAndLocationIdOrderBySortOrderAscCreatedAtAsc(locationId));
        }

        List<MenuItemEntity> items = new ArrayList<>(itemRepository.findAllByIsDeletedFalseAndLocationIdIsNullOrderByDisplayOrderAscCreatedAtAsc());
        if (locationId != null) {
            items.addAll(itemRepository.findAllByIsDeletedFalseAndLocationIdOrderByDisplayOrderAscCreatedAtAsc(locationId));
        }

        Map<OverrideKey, LocationMenuOverrideEntity> overridesByKey = locationId == null
                ? Map.of()
                : overrideRepository.findByLocationId(locationId).stream()
                        .collect(Collectors.toMap(
                                override -> new OverrideKey(override.getTargetType(), override.getTargetId()),
                                Function.identity(),
                                (left, right) -> right,
                                LinkedHashMap::new
                        ));

        Map<UUID, MenuCatalogCategoryDtoBuilder> categoryBuilders = new LinkedHashMap<>();
        for (MenuCategoryEntity category : categories) {
            LocationMenuOverrideEntity override = overridesByKey.get(new OverrideKey("CATEGORY", category.getId()));
            if (isHidden(category.getIsActive(), category.getIsDeleted(), override == null ? null : override.getIsVisible())) {
                continue;
            }
            MenuCatalogCategoryDtoBuilder builder = new MenuCatalogCategoryDtoBuilder(
                    category.getId(),
                    category.getLocationId(),
                    pickString(category.getName(), override == null ? null : override.getCustomName()),
                    pickString(category.getDescription(), override == null ? null : override.getCustomDescription()),
                    pickInteger(category.getSortOrder(), override == null ? null : override.getSortOrder())
            );
            categoryBuilders.put(category.getId(), builder);
        }

        Map<UUID, List<MenuCatalogItemDto>> itemsByCategory = new LinkedHashMap<>();
        for (MenuItemEntity item : items) {
            if (!categoryBuilders.containsKey(item.getCategoryId())) {
                continue;
            }

            LocationMenuOverrideEntity override = overridesByKey.get(new OverrideKey("ITEM", item.getId()));
            if (isHidden(resolveItemActive(item), item.getIsDeleted(), override == null ? null : override.getIsVisible())) {
                continue;
            }

            MenuCatalogItemDto resolvedItem = new MenuCatalogItemDto(
                    item.getId(),
                    item.getCategoryId(),
                    item.getLocationId(),
                    pickString(item.getName(), override == null ? null : override.getCustomName()),
                    pickString(item.getDescription(), override == null ? null : override.getCustomDescription()),
                    pickBigDecimal(item.getPrice(), override == null ? null : override.getCustomPrice()),
                    item.getSku(),
                    pickInteger(item.getDisplayOrder(), override == null ? null : override.getSortOrder()),
                    resolveVisible(item.getIsAvailable(), item.getIsDeleted(), override == null ? null : override.getIsVisible())
            );
            itemsByCategory.computeIfAbsent(item.getCategoryId(), ignored -> new ArrayList<>()).add(resolvedItem);
        }

        List<MenuCatalogCategoryDto> result = categoryBuilders.values().stream()
                .sorted(Comparator
                        .comparingInt(MenuCatalogCategoryDtoBuilder::effectiveSortOrder)
                        .thenComparing(MenuCatalogCategoryDtoBuilder::id))
                .map(builder -> builder.toDto(sortItems(itemsByCategory.getOrDefault(builder.id(), List.of()))))
                .toList();

        return new MenuCatalogResponseDto(result);
    }

    private List<MenuCatalogItemDto> sortItems(List<MenuCatalogItemDto> items) {
        return items.stream()
                .sorted(Comparator
                        .comparingInt((MenuCatalogItemDto item) -> item.displayOrder() == null ? 0 : item.displayOrder())
                        .thenComparing(MenuCatalogItemDto::id))
                .toList();
    }

    private UUID resolveLocationId(String locationCode) {
        if (locationCode == null || locationCode.isBlank()) {
            return null;
        }
        return locationRepository.findByLocationCodeIgnoreCase(locationCode.trim().toUpperCase())
                .map(LocationEntity::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Location not found: " + locationCode));
    }

    private boolean isHidden(Boolean baseEnabled, Boolean isDeleted, Boolean overrideVisible) {
        return !resolveVisible(baseEnabled, isDeleted, overrideVisible);
    }

    private Boolean resolveItemActive(MenuItemEntity item) {
        if (Boolean.FALSE.equals(item.getIsActive())) {
            return false;
        }
        return item.getIsAvailable();
    }

    private boolean resolveVisible(Boolean baseEnabled, Boolean isDeleted, Boolean overrideVisible) {
        if (Boolean.TRUE.equals(isDeleted)) {
            return false;
        }
        if (overrideVisible != null) {
            return overrideVisible;
        }
        return !Boolean.FALSE.equals(baseEnabled);
    }

    private String pickString(String baseValue, String overrideValue) {
        return overrideValue != null ? overrideValue : baseValue;
    }

    private Integer pickInteger(Integer baseValue, Integer overrideValue) {
        return overrideValue != null ? overrideValue : baseValue;
    }

    private BigDecimal pickBigDecimal(BigDecimal baseValue, BigDecimal overrideValue) {
        return overrideValue != null ? overrideValue : baseValue;
    }

    private record OverrideKey(String targetType, UUID targetId) {
        private OverrideKey {
            targetType = targetType == null ? null : targetType.trim().toUpperCase();
        }
    }

    private static final class MenuCatalogCategoryDtoBuilder {
        private final UUID id;
        private final UUID locationId;
        private final String name;
        private final String description;
        private final Integer sortOrder;

        private MenuCatalogCategoryDtoBuilder(UUID id, UUID locationId, String name, String description, Integer sortOrder) {
            this.id = id;
            this.locationId = locationId;
            this.name = name;
            this.description = description;
            this.sortOrder = sortOrder;
        }

        private UUID id() {
            return id;
        }

        private int effectiveSortOrder() {
            return sortOrder == null ? 0 : sortOrder;
        }

        private MenuCatalogCategoryDto toDto(List<MenuCatalogItemDto> items) {
            return new MenuCatalogCategoryDto(id, locationId, name, description, sortOrder, items);
        }
    }
}
