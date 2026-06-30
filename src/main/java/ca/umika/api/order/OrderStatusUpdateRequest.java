package ca.umika.api.order;

public record OrderStatusUpdateRequest(
        String status,
        String note
) {
}
