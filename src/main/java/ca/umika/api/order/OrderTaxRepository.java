package ca.umika.api.order;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTaxRepository extends JpaRepository<OrderTaxEntity, UUID> {
}
