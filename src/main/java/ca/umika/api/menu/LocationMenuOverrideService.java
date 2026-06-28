package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.store.LocationRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public LocationMenuOverrideService(
            LocationMenuOverrideRepository repository,
            LocationMenuOverrideMapper mapper,
            LocationRepository locationRepository,
            MenuCategoryRepository categoryRepository,
            MenuItemRepository itemRepository
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.locationRepository = locationRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public Page<LocationMenuOverrideDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public LocationMenuOverrideDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("LocationMenuOverride not found: " + id));
    }

    public LocationMenuOverrideDto create(LocationMenuOverrideDto dto) {
        validateLocation(dto.locationId());
        validateTarget(dto.targetType(), dto.targetId());
        LocationMenuOverrideEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public LocationMenuOverrideDto update(UUID id, LocationMenuOverrideDto dto) {
        LocationMenuOverrideEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LocationMenuOverride not found: " + id));
        validateLocation(dto.locationId());
        validateTarget(dto.targetType(), dto.targetId());
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("LocationMenuOverride not found: " + id);
        }
        repository.deleteById(id);
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
}
