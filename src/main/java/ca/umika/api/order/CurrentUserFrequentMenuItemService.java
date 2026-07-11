package ca.umika.api.order;

import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.menu.LocationMenuOverrideEntity;
import ca.umika.api.menu.LocationMenuOverrideRepository;
import ca.umika.api.menu.MenuItemEntity;
import ca.umika.api.menu.MenuItemImageEntity;
import ca.umika.api.menu.MenuItemImageRepository;
import ca.umika.api.menu.MenuItemRepository;
import ca.umika.api.menu.MenuRecommendationResponse;
import ca.umika.api.menu.MenuRecommendationService;
import ca.umika.api.store.LocationRepository;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CurrentUserFrequentMenuItemService {

    private static final int DEFAULT_LIMIT = 4;
    private static final int MAX_LIMIT = 20;
    private static final String ITEM = "ITEM";
    private static final List<String> COUNTED_STATUSES = List.of("PAID", "PREPARING", "READY", "COMPLETED");

    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemImageRepository imageRepository;
    private final LocationMenuOverrideRepository overrideRepository;
    private final MenuRecommendationService menuRecommendationService;

    public CurrentUserFrequentMenuItemService(
            OrderItemRepository orderItemRepository,
            UserRepository userRepository,
            LocationRepository locationRepository,
            MenuItemRepository menuItemRepository,
            MenuItemImageRepository imageRepository,
            LocationMenuOverrideRepository overrideRepository,
            MenuRecommendationService menuRecommendationService
    ) {
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.locationRepository = locationRepository;
        this.menuItemRepository = menuItemRepository;
        this.imageRepository = imageRepository;
        this.overrideRepository = overrideRepository;
        this.menuRecommendationService = menuRecommendationService;
    }

    public List<CurrentUserFrequentMenuItemDto> find(Authentication authentication, Integer limit, UUID locationId) {
        int resolvedLimit = resolveLimit(limit);
        if (locationId != null && !locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Location not found: " + locationId);
        }
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return recommendedItems(authentication, resolvedLimit, locationId);
        }

        UserEntity user = resolveUser(authentication);

        int queryLimit = Math.min(50, resolvedLimit * 4);
        List<CurrentUserFrequentMenuItemDto> userItems = resolveItems(
                orderItemRepository.findMostOrderedByUser(user.getId(), locationId, COUNTED_STATUSES, queryLimit),
                locationId,
                "USER_HISTORY",
                resolvedLimit
        );
        List<CurrentUserFrequentMenuItemDto> result = new ArrayList<>(userItems);
        if (result.size() >= resolvedLimit) {
            return result;
        }

        appendMissing(result, resolveItems(
                orderItemRepository.findMostOrderedGlobal(locationId, COUNTED_STATUSES, queryLimit),
                locationId,
                "GLOBAL_POPULAR",
                resolvedLimit
        ), resolvedLimit);
        if (result.size() >= resolvedLimit) {
            return result;
        }

        appendMissing(result, recommendedItems(authentication, resolvedLimit, locationId), resolvedLimit);
        return result;
    }

    private void appendMissing(
            List<CurrentUserFrequentMenuItemDto> target,
            List<CurrentUserFrequentMenuItemDto> candidates,
            int limit
    ) {
        if (target.size() >= limit || candidates.isEmpty()) {
            return;
        }
        Set<UUID> existingIds = target.stream()
                .map(CurrentUserFrequentMenuItemDto::menuItemId)
                .collect(Collectors.toSet());
        for (CurrentUserFrequentMenuItemDto candidate : candidates) {
            if (target.size() >= limit) {
                return;
            }
            if (candidate.menuItemId() != null && existingIds.add(candidate.menuItemId())) {
                target.add(candidate);
            }
        }
    }

    private List<CurrentUserFrequentMenuItemDto> recommendedItems(Authentication authentication, int limit, UUID locationId) {
        return menuRecommendationService.findPublic(authentication, locationId, limit).stream()
                .map(this::toRecommendedDto)
                .toList();
    }

    private CurrentUserFrequentMenuItemDto toRecommendedDto(MenuRecommendationResponse recommendation) {
        return new CurrentUserFrequentMenuItemDto(
                recommendation.menuItemId(),
                recommendation.categoryId(),
                recommendation.itemName(),
                recommendation.itemDescription(),
                recommendation.price(),
                recommendation.imageUrl(),
                recommendation.sku(),
                0L,
                0L,
                "RECOMMENDATION"
        );
    }

    private List<CurrentUserFrequentMenuItemDto> resolveItems(
            List<OrderItemPopularityProjection> popularity,
            UUID locationId,
            String source,
            int limit
    ) {
        if (popularity.isEmpty()) {
            return List.of();
        }

        List<UUID> menuItemIds = popularity.stream()
                .map(OrderItemPopularityProjection::getMenuItemId)
                .distinct()
                .toList();
        Map<UUID, MenuItemEntity> itemById = menuItemRepository.findAllById(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItemEntity::getId, item -> item));
        Map<UUID, String> imageByItemId = resolveImages(menuItemIds);
        Map<UUID, LocationMenuOverrideEntity> overrideByItemId = locationId == null
                ? Map.of()
                : overrideRepository.findByLocationId(locationId).stream()
                        .filter(override -> ITEM.equalsIgnoreCase(override.getTargetType()))
                        .collect(Collectors.toMap(
                                LocationMenuOverrideEntity::getTargetId,
                                override -> override,
                                (left, right) -> right,
                                LinkedHashMap::new
                        ));

        return popularity.stream()
                .map(row -> toDto(row, itemById.get(row.getMenuItemId()), imageByItemId, overrideByItemId, locationId, source))
                .filter(item -> item != null)
                .limit(limit)
                .toList();
    }

    private CurrentUserFrequentMenuItemDto toDto(
            OrderItemPopularityProjection row,
            MenuItemEntity item,
            Map<UUID, String> imageByItemId,
            Map<UUID, LocationMenuOverrideEntity> overrideByItemId,
            UUID locationId,
            String source
    ) {
        if (item == null
                || Boolean.TRUE.equals(item.getIsDeleted())
                || Boolean.FALSE.equals(item.getIsActive())
                || Boolean.FALSE.equals(item.getIsAvailable())) {
            return null;
        }
        if (locationId != null && item.getLocationId() != null && !locationId.equals(item.getLocationId())) {
            return null;
        }

        LocationMenuOverrideEntity override = overrideByItemId.get(item.getId());
        if (override != null && Boolean.FALSE.equals(override.getIsVisible())) {
            return null;
        }

        String itemName = pickString(item.getName(), override == null ? null : override.getCustomName());
        String description = pickString(item.getDescription(), override == null ? null : override.getCustomDescription());
        BigDecimal price = pickBigDecimal(item.getPrice(), override == null ? null : override.getCustomPrice());
        String imageUrl = pickString(imageByItemId.get(item.getId()), override == null ? null : override.getCustomImageUrl());

        return new CurrentUserFrequentMenuItemDto(
                item.getId(),
                item.getCategoryId(),
                itemName,
                description,
                price,
                imageUrl,
                item.getSku(),
                nullToZero(row.getTotalQuantity()),
                nullToZero(row.getOrderCount()),
                source
        );
    }

    private Map<UUID, String> resolveImages(Collection<UUID> menuItemIds) {
        if (menuItemIds.isEmpty()) {
            return Map.of();
        }
        return imageRepository.findByMenuItemIdIn(List.copyOf(menuItemIds)).stream()
                .sorted(Comparator
                        .comparing((MenuItemImageEntity image) -> !Boolean.TRUE.equals(image.getIsPrimary()))
                        .thenComparing(image -> image.getSortOrder() == null ? 0 : image.getSortOrder())
                        .thenComparing(MenuItemImageEntity::getId))
                .collect(Collectors.toMap(
                        MenuItemImageEntity::getMenuItemId,
                        MenuItemImageEntity::getImageUrl,
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }

    private UserEntity resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and " + MAX_LIMIT);
        }
        return limit;
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private String pickString(String baseValue, String overrideValue) {
        return overrideValue != null ? overrideValue : baseValue;
    }

    private BigDecimal pickBigDecimal(BigDecimal baseValue, BigDecimal overrideValue) {
        return overrideValue != null ? overrideValue : baseValue;
    }
}
