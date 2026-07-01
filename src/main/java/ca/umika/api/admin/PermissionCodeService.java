package ca.umika.api.admin;

import ca.umika.api.common.web.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PermissionCodeService {

    private final PermissionCodeRepository repository;
    private final PermissionCodeMapper mapper;

    public PermissionCodeService(PermissionCodeRepository repository, PermissionCodeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Page<PermissionCodeDto> findAll(Pageable pageable, String permissionGroup, Boolean activeOnly) {
        boolean onlyActive = activeOnly == null || activeOnly;
        if (permissionGroup != null && !permissionGroup.isBlank()) {
            String group = permissionGroup.trim().toUpperCase();
            if (onlyActive) {
                return repository.findByPermissionGroupIgnoreCaseAndIsActiveTrue(group, pageable).map(mapper::toDto);
            }
            return repository.findByPermissionGroupIgnoreCase(group, pageable).map(mapper::toDto);
        }

        if (onlyActive) {
            return repository.findByIsActiveTrue(pageable).map(mapper::toDto);
        }
        return repository.findAll(pageable).map(mapper::toDto);
    }

    public PermissionCodeDto findByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ResourceNotFoundException("Permission code not found: " + code);
        }
        return repository.findByCodeIgnoreCase(code.trim())
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Permission code not found: " + code));
    }
}
