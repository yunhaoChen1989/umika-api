package ca.umika.api.menu;

import ca.umika.api.common.web.ResourceNotFoundException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MenuCategoryService {

    private final MenuCategoryRepository repository;
    private final MenuCategoryMapper mapper;
    private final MenuAccessService menuAccessService;

    public MenuCategoryService(MenuCategoryRepository repository, MenuCategoryMapper mapper, MenuAccessService menuAccessService) {
        this.repository = repository;
        this.mapper = mapper;
        this.menuAccessService = menuAccessService;
    }

    @Transactional(readOnly = true)
    public Page<MenuCategoryDto> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDto);
    }

    @Transactional(readOnly = true)
    public MenuCategoryDto findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory not found: " + id));
    }

    public MenuCategoryDto create(Authentication authentication, MenuCategoryDto dto) {
        menuAccessService.assertWriteAccess(authentication, dto.locationId());
        MenuCategoryEntity entity = mapper.toEntity(dto);
        entity.setId(null);
        return mapper.toDto(repository.save(entity));
    }

    public MenuCategoryDto update(Authentication authentication, UUID id, MenuCategoryDto dto) {
        MenuCategoryEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory not found: " + id));
        menuAccessService.assertWriteAccess(authentication, entity.getLocationId());
        menuAccessService.assertWriteAccess(authentication, dto.locationId());
        mapper.updateEntity(entity, dto);
        return mapper.toDto(repository.save(entity));
    }

    public void delete(Authentication authentication, UUID id) {
        MenuCategoryEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuCategory not found: " + id));
        menuAccessService.assertWriteAccess(authentication, entity.getLocationId());
        repository.delete(entity);
    }
}
