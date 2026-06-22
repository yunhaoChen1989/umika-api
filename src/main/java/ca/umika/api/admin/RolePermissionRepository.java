package ca.umika.api.admin;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {
}
