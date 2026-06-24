package ca.umika.api.admin;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserPermissionService {

    private final UserPermissionRepository repository;
    private final UserPermissionMapper mapper;

    public UserPermissionService(UserPermissionRepository repository, UserPermissionMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<UserPermissionDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserPermissionDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("UserPermission not found: " + id));
    }

    public UserPermissionDto create(UserPermissionDto dto) {
        UserPermissionEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public UserPermissionDto update(UUID id, UserPermissionDto dto) {
        UserPermissionEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("UserPermission not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("UserPermission not found: " + id);
        }
        repository.deleteById(id);
    }
}
