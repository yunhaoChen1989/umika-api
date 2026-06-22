package ca.umika.api.auth;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleService {

    private final RoleRepository repository;
    private final RoleMapper mapper;

    public RoleService(RoleRepository repository, RoleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RoleDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    public RoleDto create(RoleDto dto) {
        RoleEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public RoleDto update(UUID id, RoleDto dto) {
        RoleEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Role not found: " + id);
        }
        repository.deleteById(id);
    }
}
