package ca.umika.api.user;

import ca.umika.api.auth.RoleEntity;
import ca.umika.api.auth.RoleRepository;
import ca.umika.api.auth.UserRoleEntity;
import ca.umika.api.auth.UserRoleId;
import ca.umika.api.auth.UserRoleRepository;
import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository repository,
            UserMapper mapper,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public UserDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    public UserDto create(UserWriteRequest dto) {
        UserEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        if (dto.password() == null || dto.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        entity.setPasswordHash(passwordEncoder.encode(dto.password()));
        UserEntity saved = repository.save(entity);
        syncUserRole(saved.getId(), dto.roleId(), true);
        return mapper.toDto(saved);
    }

    public UserDto update(UUID id, UserWriteRequest dto) {
        UserEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        mapper.updateEntity(entity, dto);
        if (dto.password() != null && !dto.password().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(dto.password()));
        }
        UserEntity saved = repository.save(entity);
        syncUserRole(saved.getId(), dto.roleId(), false);
        return mapper.toDto(saved);
    }

    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        userRoleRepository.deleteByUserId(id);
        repository.deleteById(id);
    }

    private void syncUserRole(UUID userId, UUID roleId, boolean useDefaultRole) {
        if (roleId == null && !useDefaultRole) {
            return;
        }

        userRoleRepository.deleteByUserId(userId);

        RoleEntity role = roleId != null
                ? roleRepository.findById(roleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleId))
                : roleRepository.findByName("ROLE_CUSTOMER")
                        .orElseThrow(() -> new ResourceNotFoundException("Default role not found: ROLE_CUSTOMER"));

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setId(new UserRoleId(userId, role.getId()));
        userRoleRepository.save(userRole);
    }
}
