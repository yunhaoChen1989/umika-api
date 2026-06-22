package ca.umika.api.admin;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RolePermissionService {

    private final RolePermissionRepository repository;
    private final RolePermissionMapper mapper;

    public RolePermissionService(RolePermissionRepository repository, RolePermissionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RolePermissionDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RolePermissionDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("RolePermission not found: " + id));
    }

    public RolePermissionDto create(RolePermissionDto dto) {
        RolePermissionEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public RolePermissionDto update(UUID id, RolePermissionDto dto) {
        RolePermissionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RolePermission not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("RolePermission not found: " + id);
        }
        repository.deleteById(id);
    }
}
