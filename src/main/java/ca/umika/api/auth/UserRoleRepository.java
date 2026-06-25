package ca.umika.api.auth;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {
    List<UserRoleEntity> findByIdUserId(UUID userId);

    @Transactional
    @Modifying
    @Query("delete from UserRoleEntity ur where ur.id.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
