package ca.umika.api.order;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderTaxRepository extends JpaRepository<OrderTaxEntity, UUID> {
    List<OrderTaxEntity> findByOrderId(UUID orderId);
}
