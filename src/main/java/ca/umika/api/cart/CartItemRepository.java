package ca.umika.api.cart;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItemEntity, UUID> {
    Page<CartItemEntity> findByCart_Id(UUID cartId, Pageable pageable);
    List<CartItemEntity> findByCart_Id(UUID cartId);
}
