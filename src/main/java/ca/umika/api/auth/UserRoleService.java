package ca.umika.api.auth;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserRoleService {

    private final UserRoleRepository repository;
    private final UserRoleMapper mapper;

    public UserRoleService(UserRoleRepository repository, UserRoleMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<UserRoleDto> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public UserRoleDto findById(UUID userId, UUID roleId) {
        UserRoleId id = new UserRoleId(userId, roleId);
        return repository.findById(id).map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("UserRole not found"));
    }

    public UserRoleDto create(UserRoleDto dto) {
        return mapper.toDto(repository.save(mapper.toEntity(dto)));
    }

    public void delete(UUID userId, UUID roleId) {
        UserRoleId id = new UserRoleId(userId, roleId);
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("UserRole not found");
        }
        repository.deleteById(id);
    }
}
