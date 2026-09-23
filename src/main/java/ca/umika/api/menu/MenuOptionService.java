package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.math.BigDecimal;
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
public class MenuOptionService {
    private final MenuOptionRepository repository;
    private final MenuOptionCategoryService categoryService;
    private final MenuAccessService menuAccessService;

    public MenuOptionService(MenuOptionRepository repository, MenuOptionCategoryService categoryService,
            MenuAccessService menuAccessService) {
        this.repository = repository;
        this.categoryService = categoryService;
        this.menuAccessService = menuAccessService;
    }

    @Transactional(readOnly = true)
    public Page<MenuOptionDto> findAll(Authentication authentication, UUID categoryId, Pageable pageable) {
        MenuOptionCategoryEntity category = categoryService.get(categoryId);
        menuAccessService.assertWriteAccess(authentication, category.getLocationId());
        return repository.findByCategoryId(categoryId, pageable).map(this::toDto);
    }

    public MenuOptionDto create(Authentication authentication, MenuOptionDto dto) {
        MenuOptionCategoryEntity category = categoryService.get(dto.categoryId());
        menuAccessService.assertWriteAccess(authentication, category.getLocationId());
        MenuOptionEntity entity = new MenuOptionEntity();
        apply(entity, dto);
        return toDto(repository.save(entity));
    }

    public MenuOptionDto update(Authentication authentication, UUID id, MenuOptionDto dto) {
        MenuOptionEntity entity = get(id);
        MenuOptionCategoryEntity currentCategory = categoryService.get(entity.getCategoryId());
        MenuOptionCategoryEntity nextCategory = categoryService.get(dto.categoryId());
        menuAccessService.assertWriteAccess(authentication, currentCategory.getLocationId());
        menuAccessService.assertWriteAccess(authentication, nextCategory.getLocationId());
        apply(entity, dto);
        return toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        MenuOptionEntity entity = get(id);
        menuAccessService.assertWriteAccess(authentication, categoryService.get(entity.getCategoryId()).getLocationId());
        repository.delete(entity);
    }

    private MenuOptionEntity get(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu option not found: " + id));
    }

    private MenuOptionDto toDto(MenuOptionEntity entity) {
        return new MenuOptionDto(entity.getId(), entity.getCategoryId(), entity.getName(), entity.getNameZh(),
                entity.getNameKo(), entity.getDescription(), entity.getDescriptionZh(), entity.getDescriptionKo(),
                entity.getPriceModifier(), entity.getSortOrder(), entity.getIsActive(), entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private void apply(MenuOptionEntity entity, MenuOptionDto dto) {
        if (dto.categoryId() == null || dto.name() == null || dto.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Option category and name are required");
        }
        entity.setCategoryId(dto.categoryId());
        entity.setName(dto.name().trim());
        entity.setNameZh(clean(dto.nameZh()));
        entity.setNameKo(clean(dto.nameKo()));
        entity.setDescription(clean(dto.description()));
        entity.setDescriptionZh(clean(dto.descriptionZh()));
        entity.setDescriptionKo(clean(dto.descriptionKo()));
        entity.setPriceModifier(dto.priceModifier() == null ? BigDecimal.ZERO : dto.priceModifier());
        entity.setSortOrder(dto.sortOrder() == null ? 0 : dto.sortOrder());
        entity.setIsActive(dto.isActive() == null || dto.isActive());
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
