package ca.umika.api.store;

import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import ca.umika.api.user.UserRepository;

@Service
@Transactional
public class LocationSettingService {

    private static final String MANAGE_PERMISSION_CODE = "LOCATION_SETTING_MANAGE";

    private final LocationSettingRepository repository;
    private final LocationRepository locationRepository;
    private final LocationSettingMapper mapper;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;

    public LocationSettingService(
            LocationSettingRepository repository,
            LocationRepository locationRepository,
            LocationSettingMapper mapper,
            UserRepository userRepository,
            UserPermissionRepository userPermissionRepository
    ) {
        this.repository = repository;
        this.locationRepository = locationRepository;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;
    }

    @Transactional(readOnly = true)
    public Page<LocationSettingDto> findAll(Authentication authentication, Pageable pageable, UUID locationId) {
        assertPermission(authentication, locationId);
        if (locationId != null) {
            return repository.findByLocationId(locationId, pageable).map(mapper::toDto);
        }
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public LocationSettingDto findById(Authentication authentication, UUID id) {
        LocationSettingEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LocationSetting not found: " + id));
        assertPermission(authentication, entity.getLocationId());
        return mapper.toDto(entity);
    }

    public LocationSettingDto create(Authentication authentication, LocationSettingDto dto) {
        assertPermission(authentication, dto.locationId());
        ensureLocationExists(dto.locationId());
        ensureUniqueSetting(dto.locationId(), dto.settingKey(), null);
        LocationSettingEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public LocationSettingDto update(Authentication authentication, UUID id, LocationSettingDto dto) {
        LocationSettingEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LocationSetting not found: " + id));
        assertPermission(authentication, entity.getLocationId());
        ensureLocationExists(dto.locationId());
        ensureUniqueSetting(dto.locationId(), dto.settingKey(), id);
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        LocationSettingEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LocationSetting not found: " + id));
        assertPermission(authentication, entity.getLocationId());
        repository.deleteById(id);
    }

    private void assertPermission(Authentication authentication, UUID locationId) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        UUID userId = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()))
                .getId();

        boolean allowed = hasGlobalManagePermission(userId)
                || (locationId != null && hasLocationManagePermission(userId, locationId));

        if (!allowed) {
            if (hasAnyManagePermissionRecord(userId, locationId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Missing location setting permission");
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing location setting permission");
        }
    }

    private boolean hasGlobalManagePermission(UUID userId) {
        return userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(
                userId, MANAGE_PERMISSION_CODE
        );
    }

    private boolean hasLocationManagePermission(UUID userId, UUID locationId) {
        return userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(
                userId, MANAGE_PERMISSION_CODE, locationId
        );
    }

    private boolean hasAnyManagePermissionRecord(UUID userId, UUID locationId) {
        return userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndLocationIdIsNull(
                userId, MANAGE_PERMISSION_CODE
        ) || (locationId != null && userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndLocationId(
                userId, MANAGE_PERMISSION_CODE, locationId
        ));
    }

    private void ensureLocationExists(UUID locationId) {
        if (locationId == null) {
            throw new ResourceNotFoundException("Location not found: null");
        }
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Location not found: " + locationId);
        }
    }

    private void ensureUniqueSetting(UUID locationId, String settingKey, UUID currentId) {
        repository.findByLocationIdAndSettingKeyIgnoreCase(locationId, settingKey)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Location setting already exists for location " + locationId + " and key " + settingKey
                    );
                });
    }
}
