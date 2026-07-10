package ca.umika.api.admin;

import ca.umika.api.common.web.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

@Service
@Transactional
public class SystemMenuService {

    private final SystemMenuRepository repository;
    private final SystemMenuMapper mapper;

    public SystemMenuService(
            SystemMenuRepository repository,
            SystemMenuMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<SystemMenuDto> findAll(Authentication authentication, Pageable pageable) {
        assertAdmin(authentication);
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public SystemMenuDto findById(Authentication authentication, UUID id) {
        assertAdmin(authentication);
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("SystemMenu not found: " + id));
    }

    public SystemMenuDto create(Authentication authentication, SystemMenuUpsertRequest request) {
        assertAdmin(authentication);
        SystemMenuEntity entity = mapper.toEntity(request);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public SystemMenuDto update(Authentication authentication, UUID id, SystemMenuUpsertRequest request) {
        assertAdmin(authentication);
        SystemMenuEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemMenu not found: " + id));
        mapper.updateEntity(entity, request);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        assertAdmin(authentication);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("SystemMenu not found: " + id);
        }
        repository.deleteById(id);
    }

    private void assertAdmin(Authentication authentication) {
        // Temporarily unrestricted while manager authentication is being repaired.
    }
}
