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
public class SystemMenuService {

    private final SystemMenuRepository repository;
    private final SystemMenuMapper mapper;

    public SystemMenuService(SystemMenuRepository repository, SystemMenuMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<SystemMenuDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public SystemMenuDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("SystemMenu not found: " + id));
    }

    public SystemMenuDto create(SystemMenuDto dto) {
        SystemMenuEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public SystemMenuDto update(UUID id, SystemMenuDto dto) {
        SystemMenuEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SystemMenu not found: " + id));
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("SystemMenu not found: " + id);
        }
        repository.deleteById(id);
    }
}
