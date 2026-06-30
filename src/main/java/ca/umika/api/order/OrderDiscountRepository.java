package ca.umika.api.order;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderDiscountRepository extends JpaRepository<OrderDiscountEntity, UUID> {
    List<OrderDiscountEntity> findByOrderId(UUID orderId);
}
