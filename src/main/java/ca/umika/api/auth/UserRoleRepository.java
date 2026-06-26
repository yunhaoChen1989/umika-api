package ca.umika.api.auth;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {
    List<UserRoleEntity> findByIdUserId(UUID userId);
    void deleteByIdUserId(UUID userId);
}
