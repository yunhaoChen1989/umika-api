package ca.umika.api.menu;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuOptionRepository extends JpaRepository<MenuOptionEntity, UUID> {
    Page<MenuOptionEntity> findByCategoryId(UUID categoryId, Pageable pageable);
    List<MenuOptionEntity> findByCategoryIdInAndIsActiveTrueOrderBySortOrderAsc(Collection<UUID> categoryIds);
}

