package ca.umika.api.printing;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import static ca.umika.api.printing.PrinterDtos.*;

/** Machine credentials are validated and location-scoped by PrinterService on every request. */
@RestController
@RequestMapping("/api/v1/print-agent")
public class PrintAgentController {
    private final PrinterService service;
    @GetMapping("/health")
    public java.util.Map<String,String> health() { return service.health(); }
    public PrintAgentController(PrinterService service) { this.service=service; }
    @PostMapping("/configure")
    public Configuration configure(@RequestHeader("X-Print-Agent-Key") String token,@Valid @RequestBody Configure request) {
        return service.configure(token,request);
    }
    @PostMapping("/poll")
    public PollResponse poll(@RequestHeader("X-Print-Agent-Key") String token,@Valid @RequestBody Poll request) {
        return service.poll(token,request);
    }
    @PostMapping("/jobs/{id}/ack")
    public void ack(@RequestHeader("X-Print-Agent-Key") String token,@PathVariable UUID id,@Valid @RequestBody Ack request) {
        service.acknowledge(token,id,request);
    }
    @PostMapping("/jobs/{id}/reprint")
    public void reprint(@RequestHeader("X-Print-Agent-Key") String token,@PathVariable UUID id,@Valid @RequestBody Reprint request) {
        service.agentReprint(token,id,request);
    }
}
