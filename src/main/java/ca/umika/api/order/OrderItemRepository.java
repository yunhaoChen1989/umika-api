package ca.umika.api.order;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, UUID> {
    List<OrderItemEntity> findByOrderId(UUID orderId);

    @Query(value = """
            SELECT
                oi.menu_item_id AS menuItemId,
                COALESCE(SUM(oi.quantity), 0) AS totalQuantity,
                COUNT(DISTINCT oi.order_id) AS orderCount
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE o.user_id = :userId
              AND oi.menu_item_id IS NOT NULL
              AND UPPER(o.status) IN (:statuses)
              AND (:locationId IS NULL OR o.location_id = :locationId)
            GROUP BY oi.menu_item_id
            ORDER BY COALESCE(SUM(oi.quantity), 0) DESC, MAX(o.created_at) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<OrderItemPopularityProjection> findMostOrderedByUser(
            @Param("userId") UUID userId,
            @Param("locationId") UUID locationId,
            @Param("statuses") List<String> statuses,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT
                oi.menu_item_id AS menuItemId,
                COALESCE(SUM(oi.quantity), 0) AS totalQuantity,
                COUNT(DISTINCT oi.order_id) AS orderCount
            FROM order_items oi
            JOIN orders o ON o.id = oi.order_id
            WHERE oi.menu_item_id IS NOT NULL
              AND UPPER(o.status) IN (:statuses)
              AND (:locationId IS NULL OR o.location_id = :locationId)
            GROUP BY oi.menu_item_id
            ORDER BY COALESCE(SUM(oi.quantity), 0) DESC, MAX(o.created_at) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<OrderItemPopularityProjection> findMostOrderedGlobal(
            @Param("locationId") UUID locationId,
            @Param("statuses") List<String> statuses,
            @Param("limit") int limit
    );
}
