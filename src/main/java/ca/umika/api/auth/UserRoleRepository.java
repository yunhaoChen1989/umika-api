package ca.umika.api.auth;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UserRoleId> {
}
