package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class MenuOptionCategoryService {
    private final MenuOptionCategoryRepository repository;
    private final MenuAccessService menuAccessService;

    public MenuOptionCategoryService(MenuOptionCategoryRepository repository, MenuAccessService menuAccessService) {
        this.repository = repository;
        this.menuAccessService = menuAccessService;
    }

    @Transactional(readOnly = true)
    public Page<MenuOptionCategoryDto> findAll(Authentication authentication, UUID locationId, Pageable pageable) {
        menuAccessService.assertWriteAccess(authentication, locationId);
        return repository.findAvailable(locationId, pageable).map(this::toDto);
    }

    public MenuOptionCategoryDto create(Authentication authentication, MenuOptionCategoryDto dto) {
        menuAccessService.assertWriteAccess(authentication, dto.locationId());
        MenuOptionCategoryEntity entity = new MenuOptionCategoryEntity();
        apply(entity, dto);
        return toDto(repository.save(entity));
    }

    public MenuOptionCategoryDto update(Authentication authentication, UUID id, MenuOptionCategoryDto dto) {
        MenuOptionCategoryEntity entity = get(id);
        menuAccessService.assertWriteAccess(authentication, entity.getLocationId());
        menuAccessService.assertWriteAccess(authentication, dto.locationId());
        apply(entity, dto);
        return toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        MenuOptionCategoryEntity entity = get(id);
        menuAccessService.assertWriteAccess(authentication, entity.getLocationId());
        repository.delete(entity);
    }

    MenuOptionCategoryEntity get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu option category not found: " + id));
    }

    MenuOptionCategoryDto toDto(MenuOptionCategoryEntity entity) {
        return new MenuOptionCategoryDto(entity.getId(), entity.getLocationId(), entity.getName(), entity.getNameZh(),
                entity.getNameKo(), entity.getDescription(), entity.getDescriptionZh(), entity.getDescriptionKo(),
                entity.getIsRequired(), entity.getMinSelect(), entity.getMaxSelect(), entity.getSortOrder(),
                entity.getIsActive(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private void apply(MenuOptionCategoryEntity entity, MenuOptionCategoryDto dto) {
        if (dto.name() == null || dto.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option category name is required");
        }
        int min = dto.minSelect() == null ? 0 : dto.minSelect();
        Integer max = dto.maxSelect();
        if (min < 0 || (max != null && (max <= 0 || max < min))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option selection limits are invalid");
        }
        boolean required = Boolean.TRUE.equals(dto.isRequired());
        if (required && min == 0) {
            min = 1;
        }
        entity.setLocationId(dto.locationId());
        entity.setName(dto.name().trim());
        entity.setNameZh(clean(dto.nameZh()));
        entity.setNameKo(clean(dto.nameKo()));
        entity.setDescription(clean(dto.description()));
        entity.setDescriptionZh(clean(dto.descriptionZh()));
        entity.setDescriptionKo(clean(dto.descriptionKo()));
        entity.setIsRequired(required);
        entity.setMinSelect(min);
        entity.setMaxSelect(max);
        entity.setSortOrder(dto.sortOrder() == null ? 0 : dto.sortOrder());
        entity.setIsActive(dto.isActive() == null || dto.isActive());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

