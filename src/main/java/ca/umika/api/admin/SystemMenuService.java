package ca.umika.api.admin;

import ca.umika.api.auth.AccountRoleService;
import ca.umika.api.common.web.ResourceNotFoundException;
import ca.umika.api.user.UserRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class SystemMenuService {

    private final SystemMenuRepository repository;
    private final SystemMenuMapper mapper;
    private final UserRepository userRepository;
    private final AccountRoleService accountRoleService;

    public SystemMenuService(
            SystemMenuRepository repository,
            SystemMenuMapper mapper,
            UserRepository userRepository,
            AccountRoleService accountRoleService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.userRepository = userRepository;
        this.accountRoleService = accountRoleService;
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
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        UUID userId = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()))
                .getId();
        accountRoleService.assertAdmin(userId);
    }
}
