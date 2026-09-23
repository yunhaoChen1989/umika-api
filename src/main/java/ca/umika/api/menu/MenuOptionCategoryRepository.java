package ca.umika.api.menu;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MenuOptionCategoryRepository extends JpaRepository<MenuOptionCategoryEntity, UUID> {
    @Query("select c from MenuOptionCategoryEntity c where "
            + "(:locationId is null and c.locationId is null) or "
            + "(:locationId is not null and (c.locationId is null or c.locationId = :locationId))")
    Page<MenuOptionCategoryEntity> findAvailable(UUID locationId, Pageable pageable);

    @Query("select c from MenuOptionCategoryEntity c where c.isActive = true and "
            + "((:locationId is null and c.locationId is null) or "
            + "(:locationId is not null and (c.locationId is null or c.locationId = :locationId)))")
    List<MenuOptionCategoryEntity> findActiveAvailable(UUID locationId);
}

