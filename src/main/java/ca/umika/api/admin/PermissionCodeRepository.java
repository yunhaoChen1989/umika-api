package ca.umika.api.admin;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionCodeRepository extends JpaRepository<PermissionCodeEntity, UUID> {
    Optional<PermissionCodeEntity> findByCodeIgnoreCase(String code);
    Page<PermissionCodeEntity> findByPermissionGroupIgnoreCase(String permissionGroup, Pageable pageable);
    Page<PermissionCodeEntity> findByIsActiveTrue(Pageable pageable);
    Page<PermissionCodeEntity> findByPermissionGroupIgnoreCaseAndIsActiveTrue(String permissionGroup, Pageable pageable);
}
