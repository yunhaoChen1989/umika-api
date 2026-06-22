package ca.umika.api.user;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<AddressEntity, UUID> {
}
