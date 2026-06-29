package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.store.LocationRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class LocationMenuOverrideService {

    private static final String CATEGORY = "CATEGORY";
    private static final String ITEM = "ITEM";

    private final LocationMenuOverrideRepository repository;
    private final LocationMenuOverrideMapper mapper;
    private final LocationRepository locationRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final MenuItemImageStorageService storageService;
    private final MenuAccessService menuAccessService;

    public LocationMenuOverrideService(
            LocationMenuOverrideRepository repository,
            LocationMenuOverrideMapper mapper,
            LocationRepository locationRepository,
            MenuCategoryRepository categoryRepository,
            MenuItemRepository itemRepository,
            MenuItemImageStorageService storageService,
            MenuAccessService menuAccessService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.locationRepository = locationRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.storageService = storageService;
        this.menuAccessService = menuAccessService;
    }

    @Transactional(readOnly = true)
    public Page<LocationMenuOverrideDto> findAll(Authentication authentication, Pageable pageable, UUID locationId) {
        MenuAccessService.MenuAccessContext access = menuAccessService.assertReadAccess(authentication, locationId);
        if (access.locationId() == null) {
            return repository.findAll(pageable).map(mapper::toDto);
        }
        return repository.findByLocationId(access.locationId(), pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public LocationMenuOverrideDto findById(Authentication authentication, UUID id) {
        LocationMenuOverrideEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LocationMenuOverride not found: " + id));
        menuAccessService.assertReadAccess(authentication, entity.getLocationId());
        return mapper.toDto(entity);
    }

    public LocationMenuOverrideDto create(Authentication authentication, LocationMenuOverrideDto dto) {
        menuAccessService.assertWriteAccess(authentication, dto.locationId());
        validateLocation(dto.locationId());
        validateOverrideFields(dto);
        validateTarget(dto.targetType(), dto.targetId());
        LocationMenuOverrideEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public LocationMenuOverrideDto update(Authentication authentication, UUID id, LocationMenuOverrideDto dto) {
        LocationMenuOverrideEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LocationMenuOverride not found: " + id));
        menuAccessService.assertWriteAccess(authentication, entity.getLocationId());
        menuAccessService.assertWriteAccess(authentication, dto.locationId());
        validateLocation(dto.locationId());
        validateOverrideFields(dto);
        validateTarget(dto.targetType(), dto.targetId());
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public LocationMenuOverrideDto upsert(Authentication authentication, LocationMenuOverrideDto dto) {
        menuAccessService.assertWriteAccess(authentication, dto.locationId());
        validateLocation(dto.locationId());
        validateOverrideFields(dto);
        validateTarget(dto.targetType(), dto.targetId());
        String targetType = normalizeTargetType(dto.targetType());
        LocationMenuOverrideEntity entity = repository
                .findByLocationIdAndTargetTypeAndTargetId(dto.locationId(), targetType, dto.targetId())
                .orElseGet(LocationMenuOverrideEntity::new);
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public LocationMenuOverrideDto uploadItemOverrideImage(
            Authentication authentication,
            UUID locationId,
            UUID menuItemId,
            MultipartFile file,
            String publicBaseUrl
    ) {
        menuAccessService.assertWriteAccess(authentication, locationId);
        validateLocation(locationId);
        validateTarget(ITEM, menuItemId);
        String filename = storageService.store(file);
        String imageUrl = buildPublicUrl(publicBaseUrl, filename);

        LocationMenuOverrideEntity entity = repository
                .findByLocationIdAndTargetTypeAndTargetId(locationId, ITEM, menuItemId)
                .orElseGet(LocationMenuOverrideEntity::new);
        entity.setLocationId(locationId);
        entity.setTargetType(ITEM);
        entity.setTargetId(menuItemId);
        entity.setCustomImageUrl(imageUrl);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        LocationMenuOverrideEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LocationMenuOverride not found: " + id));
        menuAccessService.assertWriteAccess(authentication, entity.getLocationId());
        repository.delete(entity);
    }

    private void validateLocation(UUID locationId) {
        if (locationId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "locationId is required");
        }
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Location not found: " + locationId);
        }
    }

    private void validateTarget(String targetType, UUID targetId) {
        String normalizedTargetType = normalizeTargetType(targetType);
        if (normalizedTargetType == null || targetId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetType and targetId are required");
        }

        if (CATEGORY.equals(normalizedTargetType)) {
            if (!categoryRepository.existsById(targetId)) {
                throw new ResourceNotFoundException("MenuCategory not found: " + targetId);
            }
            return;
        }

        if (ITEM.equals(normalizedTargetType)) {
            if (!itemRepository.existsById(targetId)) {
                throw new ResourceNotFoundException("MenuItem not found: " + targetId);
            }
            return;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "targetType must be CATEGORY or ITEM");
    }

    private String normalizeTargetType(String targetType) {
        return targetType == null ? null : targetType.trim().toUpperCase();
    }

    private void validateOverrideFields(LocationMenuOverrideDto dto) {
        if (CATEGORY.equals(normalizeTargetType(dto.targetType()))
                && (dto.customPrice() != null || dto.customImageUrl() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "customPrice and customImageUrl are only valid for ITEM overrides");
        }
    }

    private String buildPublicUrl(String publicBaseUrl, String filename) {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/uploads/menu-item-images/" + filename;
    }
}
