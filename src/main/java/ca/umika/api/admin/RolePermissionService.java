package ca.umika.api.admin;

import ca.umika.api.common.web.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RolePermissionService {

    private final RolePermissionRepository repository;
    private final RolePermissionMapper mapper;
    private final PermissionCodeRepository permissionCodeRepository;

    public RolePermissionService(
            RolePermissionRepository repository,
            RolePermissionMapper mapper,
            PermissionCodeRepository permissionCodeRepository
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.permissionCodeRepository = permissionCodeRepository;
    }

    @Transactional(readOnly = true)
    public Page<RolePermissionDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public RolePermissionDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("RolePermission not found: " + id));
    }

    public RolePermissionDto create(RolePermissionDto dto) {
        String permissionCode = normalizeAndValidatePermissionCode(dto.permissionCode());
        RolePermissionEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        entity.setPermissionCode(permissionCode);
        return mapper.toDto(repository.save(entity));
    }

    public RolePermissionDto update(UUID id, RolePermissionDto dto) {
        RolePermissionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RolePermission not found: " + id));
        String permissionCode = normalizeAndValidatePermissionCode(dto.permissionCode());
        mapper.updateEntity(entity, dto);
        entity.setPermissionCode(permissionCode);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("RolePermission not found: " + id);
        }
        repository.deleteById(id);
    }

    private String normalizeAndValidatePermissionCode(String permissionCode) {
        if (permissionCode == null || permissionCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "permissionCode is required");
        }
        String normalized = permissionCode.trim().toUpperCase();
        PermissionCodeEntity catalog = permissionCodeRepository.findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown permissionCode: " + permissionCode));
        if (!Boolean.TRUE.equals(catalog.getIsActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission code is inactive: " + normalized);
        }
        return catalog.getCode();
    }
}
