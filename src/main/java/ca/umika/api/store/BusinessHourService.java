package ca.umika.api.store;

import ca.umika.api.admin.UserPermissionRepository;
import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.common.web.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ca.umika.api.user.UserEntity;
import ca.umika.api.user.UserRepository;

@Service
@Transactional
public class BusinessHourService {

    private static final String MANAGE_PERMISSION_CODE = "BUSINESS_HOUR_MANAGE";

    private final BusinessHourRepository repository;
    private final LocationRepository locationRepository;
    private final BusinessHourMapper mapper;
    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final AccountRoleService accountRoleService;

    public BusinessHourService(
            BusinessHourRepository repository,
            LocationRepository locationRepository,
            BusinessHourMapper mapper,
            UserRepository userRepository,
            UserPermissionRepository userPermissionRepository,
            AccountRoleService accountRoleService
    ) {
        this.repository = repository;
        this.locationRepository = locationRepository;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.userPermissionRepository = userPermissionRepository;
        this.accountRoleService = accountRoleService;
    }

    @Transactional(readOnly = true)
    public Page<BusinessHourDto> findAll(Pageable pageable, UUID locationId) {
        if (locationId != null) {
            ensureLocationExists(locationId);
            return repository.findByLocationId(locationId, pageable).map(mapper::toDto);
        }
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public BusinessHourDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessHour not found: " + id));
    }

    public BusinessHourDto create(Authentication authentication, BusinessHourDto dto) {
        validateDto(dto, null);
        assertManagePermission(authentication, dto.locationId());
        BusinessHourEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public BusinessHourDto update(Authentication authentication, UUID id, BusinessHourDto dto) {
        BusinessHourEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessHour not found: " + id));
        assertManagePermission(authentication, entity.getLocationId());
        validateDto(dto, id);
        assertManagePermission(authentication, dto.locationId());
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        BusinessHourEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessHour not found: " + id));
        assertManagePermission(authentication, entity.getLocationId());
        repository.deleteById(id);
    }

    private void validateDto(BusinessHourDto dto, UUID currentId) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Business hour payload is required");
        }
        ensureLocationExists(dto.locationId());
        if (dto.dayOfWeek() == null || dto.dayOfWeek() < 0 || dto.dayOfWeek() > 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dayOfWeek must be between 0 and 6");
        }

        boolean closed = Boolean.TRUE.equals(dto.isClosed());
        if (!closed) {
            if (dto.openTime() == null || dto.closeTime() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "openTime and closeTime are required when store is open");
            }
            if (!dto.closeTime().isAfter(dto.openTime())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "closeTime must be after openTime");
            }
        }

        repository.findByLocationIdAndDayOfWeek(dto.locationId(), dto.dayOfWeek())
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Business hour already exists for this location and day");
                });
    }

    private void assertManagePermission(Authentication authentication, UUID locationId) {
        UserEntity user = resolveUser(authentication);
        if (accountRoleService.resolveRoleNames(user.getId()).contains("ROLE_ADMIN")) {
            return;
        }
        boolean globalPermission = userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(
                user.getId(), MANAGE_PERMISSION_CODE
        );
        boolean locationPermission = locationId != null && userPermissionRepository.existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(
                user.getId(), MANAGE_PERMISSION_CODE, locationId
        );
        if (!globalPermission && !locationPermission) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing business hour permission");
        }
    }

    private UserEntity resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));
    }

    private void ensureLocationExists(UUID locationId) {
        if (locationId == null || !locationRepository.existsById(locationId)) {
            throw new ResourceNotFoundException("Location not found: " + locationId);
        }
    }
}
