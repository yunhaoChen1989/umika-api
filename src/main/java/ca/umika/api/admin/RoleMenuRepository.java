package ca.umika.api.admin;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleMenuRepository extends JpaRepository<RoleMenuEntity, RoleMenuId> {
    List<RoleMenuEntity> findByIdRoleIdIn(Collection<UUID> roleIds);
}
