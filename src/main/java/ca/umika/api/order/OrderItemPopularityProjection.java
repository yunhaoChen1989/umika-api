package ca.umika.api.order;

import java.util.UUID;

public interface OrderItemPopularityProjection {
    UUID getMenuItemId();
    Long getTotalQuantity();
    Long getOrderCount();
}
