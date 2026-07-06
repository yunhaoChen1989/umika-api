package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MenuRecommendationService {

    private static final int DEFAULT_LIMIT = 6;
    private static final String ITEM = "ITEM";

    private final MenuRecommendationRepository repository;
    private final MenuRecommendationMapper mapper;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemImageRepository imageRepository;
    private final LocationMenuOverrideRepository overrideRepository;
    private final MenuAccessService menuAccessService;

    public MenuRecommendationService(
            MenuRecommendationRepository repository,
            MenuRecommendationMapper mapper,
            MenuItemRepository menuItemRepository,
            MenuItemImageRepository imageRepository,
            LocationMenuOverrideRepository overrideRepository,
            MenuAccessService menuAccessService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.menuItemRepository = menuItemRepository;
        this.imageRepository = imageRepository;
        this.overrideRepository = overrideRepository;
        this.menuAccessService = menuAccessService;
    }

    @Transactional(readOnly = true)
    public List<MenuRecommendationResponse> findPublic(Authentication authentication, UUID locationId, Integer limit) {
        menuAccessService.assertReadAccess(authentication, locationId);
        int resolvedLimit = resolveLimit(limit);

        List<MenuRecommendationEntity> recommendations = findPublicRecommendations(locationId);

        return resolveResponses(recommendations, locationId).stream()
                .limit(resolvedLimit)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MenuRecommendationManageResponse> findManage(Authentication authentication, Pageable pageable, UUID locationId) {
        menuAccessService.assertWriteAccess(authentication, locationId);
        if (locationId == null) {
            return repository.findByLocationIdIsNull(pageable)
                    .map(recommendation -> toManageResponse(recommendation, null, Map.of()));
        }
        Map<UUID, LocationMenuOverrideEntity> overrideByItemId = findItemOverrides(locationId);
        List<MenuRecommendationManageResponse> effectiveRecommendations = findManageRecommendations(locationId, pageable.getSort()).stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                MenuRecommendationEntity::getMenuItemId,
                                recommendation -> toManageResponse(recommendation, locationId, overrideByItemId),
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ),
                        map -> List.copyOf(map.values())
                ));
        int start = Math.min((int) pageable.getOffset(), effectiveRecommendations.size());
        int end = Math.min(start + pageable.getPageSize(), effectiveRecommendations.size());
        return new PageImpl<>(
                effectiveRecommendations.subList(start, end),
                pageable,
                effectiveRecommendations.size()
        );
    }

    @Transactional(readOnly = true)
    public MenuRecommendationDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("MenuRecommendation not found: " + id));
    }

    public MenuRecommendationDto create(Authentication authentication, MenuRecommendationDto dto) {
        validateDto(dto);
        MenuAccessService.MenuAccessContext access = menuAccessService.assertWriteAccess(authentication, dto.locationId());
        validateActiveChange(access, dto.isActive());
        ensureMenuItemExists(dto.menuItemId());
        MenuRecommendationEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        if (entity.getIsActive() == null) {
            entity.setIsActive(Boolean.TRUE);
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        return mapper.toDto(repository.save(entity));
    }

    public MenuRecommendationDto update(Authentication authentication, UUID id, MenuRecommendationDto dto) {
        validateDto(dto);
        MenuRecommendationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuRecommendation not found: " + id));
        MenuAccessService.MenuAccessContext access = menuAccessService.assertWriteAccess(authentication, entity.getLocationId());
        menuAccessService.assertWriteAccess(authentication, dto.locationId());
        validateActiveChange(access, dto.isActive());
        ensureMenuItemExists(dto.menuItemId());
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        MenuRecommendationEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuRecommendation not found: " + id));
        menuAccessService.assertWriteAccess(authentication, entity.getLocationId());
        repository.delete(entity);
    }

    private List<MenuRecommendationEntity> findPublicRecommendations(UUID locationId) {
        List<MenuRecommendationEntity> recommendations = new ArrayList<>();
        if (locationId != null) {
            recommendations.addAll(repository.findByIsActiveTrueAndLocationIdOrderBySortOrderAscCreatedAtAsc(locationId));
        }
        recommendations.addAll(repository.findByIsActiveTrueAndLocationIdIsNullOrderBySortOrderAscCreatedAtAsc());
        return recommendations;
    }

    private List<MenuRecommendationEntity> findManageRecommendations(UUID locationId, Sort sort) {
        Sort resolvedSort = sort.isSorted() ? sort : Sort.by(Sort.Order.asc("sortOrder"), Sort.Order.asc("createdAt"));
        List<MenuRecommendationEntity> recommendations = new ArrayList<>();
        recommendations.addAll(repository.findByLocationId(locationId, resolvedSort));
        recommendations.addAll(repository.findByLocationIdIsNull(resolvedSort));
        return recommendations;
    }

    private MenuRecommendationManageResponse toManageResponse(
            MenuRecommendationEntity recommendation,
            UUID requestedLocationId,
            Map<UUID, LocationMenuOverrideEntity> overrideByItemId
    ) {
        LocationMenuOverrideEntity override = requestedLocationId == null
                ? null
                : overrideByItemId.get(recommendation.getMenuItemId());
        Boolean locationItemVisible = requestedLocationId == null
                ? null
                : !Boolean.FALSE.equals(override == null ? null : override.getIsVisible());

        return new MenuRecommendationManageResponse(
                recommendation.getId(),
                recommendation.getLocationId(),
                recommendation.getMenuItemId(),
                recommendation.getTitle(),
                recommendation.getSubtitle(),
                recommendation.getSortOrder(),
                recommendation.getIsActive(),
                locationItemVisible,
                recommendation.getCreatedAt(),
                recommendation.getUpdatedAt()
        );
    }

    public MenuRecommendationVisibilityResponse updateLocationVisibility(
            Authentication authentication,
            MenuRecommendationVisibilityRequest request
    ) {
        if (request == null || request.locationId() == null || request.menuItemId() == null || request.isVisible() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "locationId, menuItemId and isVisible are required");
        }

        menuAccessService.assertWriteAccess(authentication, request.locationId());
        ensureMenuItemExists(request.menuItemId());

        LocationMenuOverrideEntity override = overrideRepository
                .findByLocationIdAndTargetTypeAndTargetId(request.locationId(), ITEM, request.menuItemId())
                .orElseGet(LocationMenuOverrideEntity::new);
        override.setLocationId(request.locationId());
        override.setTargetType(ITEM);
        override.setTargetId(request.menuItemId());
        override.setIsVisible(request.isVisible());

        LocationMenuOverrideEntity saved = overrideRepository.save(override);
        return new MenuRecommendationVisibilityResponse(
                saved.getLocationId(),
                saved.getTargetId(),
                saved.getIsVisible(),
                saved.getUpdatedAt()
        );
    }

    private List<MenuRecommendationResponse> resolveResponses(List<MenuRecommendationEntity> recommendations, UUID locationId) {
        if (recommendations.isEmpty()) {
            return List.of();
        }

        List<UUID> menuItemIds = recommendations.stream()
                .map(MenuRecommendationEntity::getMenuItemId)
                .distinct()
                .toList();
        Map<UUID, MenuItemEntity> itemById = menuItemRepository.findAllById(menuItemIds).stream()
                .collect(Collectors.toMap(MenuItemEntity::getId, item -> item));
        Map<UUID, String> imageByItemId = resolveImages(menuItemIds);
        Map<UUID, LocationMenuOverrideEntity> overrideByItemId = locationId == null
                ? Map.of()
                : findItemOverrides(locationId);

        return recommendations.stream()
                .sorted(Comparator
                        .comparing((MenuRecommendationEntity recommendation) -> recommendation.getLocationId() == null ? 1 : 0)
                        .thenComparing(recommendation -> recommendation.getSortOrder() == null ? 0 : recommendation.getSortOrder())
                        .thenComparing(MenuRecommendationEntity::getCreatedAt))
                .map(recommendation -> toResponse(recommendation, itemById.get(recommendation.getMenuItemId()), imageByItemId, overrideByItemId))
                .filter(response -> response != null)
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                MenuRecommendationResponse::menuItemId,
                                response -> response,
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ),
                        map -> List.copyOf(map.values())
                ));
    }

    private Map<UUID, LocationMenuOverrideEntity> findItemOverrides(UUID locationId) {
        return overrideRepository.findByLocationId(locationId).stream()
                .filter(override -> ITEM.equalsIgnoreCase(override.getTargetType()))
                .collect(Collectors.toMap(
                        LocationMenuOverrideEntity::getTargetId,
                        override -> override,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private MenuRecommendationResponse toResponse(
            MenuRecommendationEntity recommendation,
            MenuItemEntity item,
            Map<UUID, String> imageByItemId,
            Map<UUID, LocationMenuOverrideEntity> overrideByItemId
    ) {
        if (item == null || Boolean.TRUE.equals(item.getIsDeleted()) || Boolean.FALSE.equals(item.getIsActive()) || Boolean.FALSE.equals(item.getIsAvailable())) {
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

        return new MenuRecommendationResponse(
                recommendation.getId(),
                recommendation.getLocationId(),
                recommendation.getMenuItemId(),
                pickString(itemName, recommendation.getTitle()),
                pickString(description, recommendation.getSubtitle()),
                recommendation.getSortOrder(),
                recommendation.getIsActive(),
                item.getCategoryId(),
                itemName,
                description,
                price,
                imageUrl,
                item.getSku(),
                item.getIsAvailable(),
                recommendation.getCreatedAt(),
                recommendation.getUpdatedAt()
        );
    }

    private Map<UUID, String> resolveImages(List<UUID> menuItemIds) {
        if (menuItemIds.isEmpty()) {
            return Map.of();
        }
        return imageRepository.findByMenuItemIdIn(menuItemIds).stream()
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

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limit must be between 1 and 50");
        }
        return limit;
    }

    private void validateDto(MenuRecommendationDto dto) {
        if (dto == null || dto.menuItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "menuItemId is required");
        }
    }

    private void validateActiveChange(MenuAccessService.MenuAccessContext access, Boolean isActive) {
        if (Boolean.FALSE.equals(isActive) && !access.admin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin users can set recommendation items inactive");
        }
    }

    private void ensureMenuItemExists(UUID menuItemId) {
        if (!menuItemRepository.existsById(menuItemId)) {
            throw new ResourceNotFoundException("MenuItem not found: " + menuItemId);
        }
    }

    private String pickString(String baseValue, String overrideValue) {
        return overrideValue != null ? overrideValue : baseValue;
    }

    private BigDecimal pickBigDecimal(BigDecimal baseValue, BigDecimal overrideValue) {
        return overrideValue != null ? overrideValue : baseValue;
    }
}
