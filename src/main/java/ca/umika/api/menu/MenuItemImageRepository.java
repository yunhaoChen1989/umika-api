package ca.umika.api.menu;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemImageRepository extends JpaRepository<MenuItemImageEntity, UUID> {
}
