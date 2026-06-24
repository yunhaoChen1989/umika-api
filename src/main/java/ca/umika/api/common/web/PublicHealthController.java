package ca.umika.api.common.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
@Tag(name = "Public")
public class PublicHealthController {

    @GetMapping("/ping")
    @Operation(summary = "Check whether the API is responding")
    ApiResult<Map<String, Object>> ping() {
        return ApiResult.ok(Map.of(
                "service", "umika-api",
                "status", "ok",
                "timestamp", Instant.now().toString()
        ));
    }
}
