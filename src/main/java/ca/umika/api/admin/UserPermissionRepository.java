package ca.umika.api.admin;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPermissionRepository extends JpaRepository<UserPermissionEntity, UUID> {
    boolean existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationId(UUID userId, String permissionCode, UUID locationId);
    boolean existsByUserIdAndPermissionCodeIgnoreCaseAndIsGrantedTrueAndLocationIdIsNull(UUID userId, String permissionCode);
}
