package ca.umika.api.printing;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import static ca.umika.api.printing.PrinterDtos.*;

@RestController
@RequestMapping("/api/v1/manager/printing/{locationId}")
public class PrinterController {
    private final PrinterService service; private final PrinterAccess access;
    public PrinterController(PrinterService service, PrinterAccess access) { this.service=service; this.access=access; }
    @GetMapping
    public Configuration get(Authentication auth,@PathVariable UUID locationId) {
        access.require(auth,locationId); return service.configuration(locationId);
    }
    @PostMapping("/pair")
    public Pairing pair(Authentication auth,@PathVariable UUID locationId) {
        access.require(auth,locationId); return service.pair(locationId);
    }
    @PutMapping("/routing")
    public Configuration routing(Authentication auth,@PathVariable UUID locationId,@Valid @RequestBody Routing request) {
        access.require(auth,locationId); return service.saveRouting(locationId,request);
    }
    @GetMapping("/receipt-template")
    public ReceiptTemplate receiptTemplate(Authentication auth,@PathVariable UUID locationId) {
        access.require(auth,locationId); return service.receiptTemplate(locationId);
    }
    @PutMapping("/receipt-template")
    public ReceiptTemplate receiptTemplate(Authentication auth,@PathVariable UUID locationId,@Valid @RequestBody ReceiptTemplate request) {
        access.require(auth,locationId); return service.saveReceiptTemplate(locationId,request);
    }
    @PostMapping("/orders/{orderId}/reprint")
    public void printOrder(Authentication auth,@PathVariable UUID locationId,@PathVariable UUID orderId) {
        access.requireOrder(auth,locationId); service.reprintOrder(locationId,orderId);
    }
    @GetMapping("/jobs")
    public List<Job> jobs(Authentication auth,@PathVariable UUID locationId) {
        access.require(auth,locationId); return service.jobs(locationId);
    }
    @PostMapping("/jobs/{jobId}/reprint")
    public void reprint(Authentication auth,@PathVariable UUID locationId,@PathVariable UUID jobId) {
        access.require(auth,locationId); service.reprint(locationId,jobId);
    }
}
