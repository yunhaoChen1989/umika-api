package ca.umika.api.printing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PrinterDtos {
    private PrinterDtos() {}
    public record Printer(@NotNull UUID id, @NotBlank @Size(max=80) String name, boolean enabled) {}
    public record Settings(@Min(1) @Max(60) int pollSeconds, @Min(5) @Max(120) int staleMinutes,
                           boolean alarmEnabled, @NotNull @Size(max=16) List<@NotNull @Valid Printer> printers) {}
    public record Configuration(UUID locationId, String locationName, int pollSeconds, int staleMinutes, boolean alarmEnabled,
                                List<Printer> printers, Instant lastSeen, boolean paired,
                                Map<String, Object> agentStatus, boolean routingManaged, UUID wholeOrderPrinterId,
                                boolean autoWholeOrder, List<ItemRoute> itemRoutes, List<CategoryRoute> categoryRoutes) {}
    public record ItemRoute(@NotNull UUID menuItemId, @NotNull UUID printerId) {}
    public record CategoryRoute(@NotNull UUID categoryId, @NotNull UUID printerId) {}
    public record ReceiptTemplate(@NotNull @Size(max=80) String headerText,
                                  @NotNull @Size(max=160) String footerText,
                                  @NotNull @Pattern(regexp="COMPACT|STANDARD|LARGE") String fontSize,
                                  boolean showCustomerPhone, boolean showPlacedTime,
                                  boolean showItemPrices, boolean showSubtotal,
                                  boolean showDiscount, boolean showTax, boolean showTip,
                                  boolean showStationItemCount, boolean showOrderTotal) {}
    public record Routing(@NotNull @Size(min=1,max=16) List<@NotNull @Valid Printer> printers,
                          @NotNull UUID wholeOrderPrinterId, boolean autoWholeOrder,
                          @NotNull @Size(max=5000) List<@NotNull @Valid ItemRoute> itemRoutes,
                          @NotNull @Size(max=500) List<@NotNull @Valid CategoryRoute> categoryRoutes) {
        public Routing(List<Printer> printers,UUID wholeOrderPrinterId,boolean autoWholeOrder,List<ItemRoute> itemRoutes) {
            this(printers,wholeOrderPrinterId,autoWholeOrder,itemRoutes,List.of());
        }
    }
    public record Job(UUID id, UUID orderId, UUID printerId, String printerName,
                      String state, boolean reprint, Instant createdAt, Map<String, Object> receipt) {}
    public record Poll(@NotNull UUID instanceId, @NotNull @Size(max=16) Map<String, String> printerStatus) {}
    public record PollResponse(Configuration configuration, List<Job> jobs) {}
    public record Ack(@NotNull UUID instanceId, @NotBlank String state) {}
    public record Pairing(String token) {}
    public record Reprint(@NotNull UUID instanceId) {}
    public record Configure(@NotNull UUID instanceId, @NotNull @Valid Settings settings) {}
}
