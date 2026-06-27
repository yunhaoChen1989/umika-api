package ca.umika.api.store;

import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.admin.UserPermissionEntity;
import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
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
    public Page<LocationSettingDto> findAll(Authentication authentication, Pageable pageable, UUID locationId, String settingGroup) {
        String normalizedGroup = normalizeSettingGroup(settingGroup);
        if (locationId != null) {
            assertPermission(authentication, locationId);
            if (normalizedGroup != null) {
                return repository.findByLocationIdAndSettingGroupIgnoreCase(locationId, normalizedGroup, pageable).map(mapper::toDto);
            }
            return repository.findByLocationId(locationId, pageable).map(mapper::toDto);
        }

        UUID userId = resolveUserId(authentication);
        if (hasGlobalManagePermission(userId)) {
            if (normalizedGroup != null) {
                return repository.findBySettingGroupIgnoreCase(normalizedGroup, pageable).map(mapper::toDto);
            }
            return repository.findAll(pageable).map(mapper::toDto);
        }

        List<UUID> allowedLocationIds = resolveAllowedLocationIds(userId);
        if (allowedLocationIds.isEmpty()) {
            if (hasAnyManagePermissionRecord(userId, null)) {
                return Page.empty(pageable);
            }
            throw unauthorized();
        }

        if (normalizedGroup != null) {
            return repository.findByLocationIdInAndSettingGroupIgnoreCase(allowedLocationIds, normalizedGroup, pageable).map(mapper::toDto);
        }
        return repository.findByLocationIdIn(allowedLocationIds, pageable).map(mapper::toDto);
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
        String normalizedGroup = normalizeSettingGroup(dto.settingGroup());
        ensureUniqueSetting(dto.locationId(), normalizedGroup, dto.settingKey(), null);
        LocationSettingEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        entity.setSettingGroup(normalizedGroup);
        return mapper.toDto(repository.save(entity));
    }

    public LocationSettingDto update(Authentication authentication, UUID id, LocationSettingDto dto) {
        LocationSettingEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LocationSetting not found: " + id));
        assertPermission(authentication, entity.getLocationId());
        ensureLocationExists(dto.locationId());
        String normalizedGroup = normalizeSettingGroup(dto.settingGroup());
        ensureUniqueSetting(dto.locationId(), normalizedGroup, dto.settingKey(), id);
        mapper.updateEntity(entity, dto);
        entity.setSettingGroup(normalizedGroup);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        LocationSettingEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LocationSetting not found: " + id));
        assertPermission(authentication, entity.getLocationId());
        repository.deleteById(id);
    }

    private void assertPermission(Authentication authentication, UUID locationId) {
        UUID userId = resolveUserId(authentication);

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
        if (userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndLocationIdIsNull(
                userId, MANAGE_PERMISSION_CODE
        )) {
            return true;
        }

        if (locationId != null && userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndLocationId(
                userId, MANAGE_PERMISSION_CODE, locationId
        )) {
            return true;
        }

        return !userPermissionRepository.findByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrue(
                userId, MANAGE_PERMISSION_CODE
        ).isEmpty();
    }

    private List<UUID> resolveAllowedLocationIds(UUID userId) {
        Set<UUID> locationIds = new LinkedHashSet<>();
        for (UserPermissionEntity permission : userPermissionRepository
                .findByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrue(userId, MANAGE_PERMISSION_CODE)) {
            if (permission.getLocationId() == null) {
                return List.of();
            }
            locationIds.add(permission.getLocationId());
        }
        return locationIds.stream().toList();
    }

    private String normalizeSettingGroup(String settingGroup) {
        if (settingGroup == null || settingGroup.isBlank()) {
            return null;
        }
        return settingGroup.trim().toUpperCase();
    }

    private UUID resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw unauthorized();
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()))
                .getId();
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing location setting permission");
    }

    private void ensureLocationExists(UUID locationId) {
        if (locationId == null) {
            throw new ResourceNotFoundException("Location not found: null");
        }
        if (!locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Location not found: " + locationId);
        }
    }

    private void ensureUniqueSetting(UUID locationId, String settingGroup, String settingKey, UUID currentId) {
        repository.findByLocationIdAndSettingGroupIgnoreCaseAndSettingKeyIgnoreCase(locationId, settingGroup, settingKey)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Location setting already exists for location " + locationId + ", group " + settingGroup + " and key " + settingKey
                    );
                });
    }
}
