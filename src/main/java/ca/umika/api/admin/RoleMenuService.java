package ca.umika.api.admin;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RoleMenuService {

    private final RoleMenuRepository repository;
    private final RoleMenuMapper mapper;

    public RoleMenuService(RoleMenuRepository repository, RoleMenuMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<RoleMenuDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RoleMenuDto findById(UUID roleId, UUID menuId) {
        RoleMenuId id = new RoleMenuId(roleId, menuId);
        return repository.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("RoleMenu not found"));
    }

    public RoleMenuDto create(RoleMenuDto dto) {
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    public void delete(UUID roleId, UUID menuId) {
        RoleMenuId id = new RoleMenuId(roleId, menuId);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("RoleMenu not found");
        }
        repository.deleteById(id);
    }
}
