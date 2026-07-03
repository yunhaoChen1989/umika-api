package ca.umika.api.menu;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuItemRepository extends JpaRepository<MenuItemEntity, UUID> {
    List<MenuItemEntity> findAllByIsDeletedFalseAndLocationIdIsNullOrderByDisplayOrderAscCreatedAtAsc();
    List<MenuItemEntity> findAllByIsDeletedFalseAndLocationIdOrderByDisplayOrderAscCreatedAtAsc(UUID locationId);

    @Query(value = "select count(*) > 0 from menu_items where id = :id", nativeQuery = true)
    boolean existsMenuItemRowById(@Param("id") UUID id);

    @Query(value = "select location_id from menu_items where id = :id", nativeQuery = true)
    Optional<UUID> findLocationIdByMenuItemId(@Param("id") UUID id);
}
