package ca.umika.api.admin;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemMenuRepository extends JpaRepository<SystemMenuEntity, UUID> {
    List<SystemMenuEntity> findByIdIn(Collection<UUID> ids);
}
