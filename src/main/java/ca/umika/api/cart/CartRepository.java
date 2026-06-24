package ca.umika.api.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CartRepository extends JpaRepository<CartEntity, UUID> {
    // Additional query methods can be added here if needed
}
