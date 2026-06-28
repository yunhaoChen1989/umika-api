package ca.umika.api.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepository extends JpaRepository<MenuCategoryEntity, UUID> {
    List<MenuCategoryEntity> findAllByIsDeletedFalseAndLocationIdIsNullOrderBySortOrderAscCreatedAtAsc();
    List<MenuCategoryEntity> findAllByIsDeletedFalseAndLocationIdOrderBySortOrderAscCreatedAtAsc(UUID locationId);
}
