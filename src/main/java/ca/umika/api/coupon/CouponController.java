package ca.umika.api.coupon;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/manager/coupons", "/api/v1/coupons"})
@Tag(name = "Coupons")
public class CouponController {

    private final CouponService service;

    public CouponController(CouponService service) {
        this.service = service;
    }

    @GetMapping
    public Page<CouponDto> findAll(
            Authentication authentication,
            Pageable pageable,
            @RequestParam(required = false) UUID locationId
    ) {
        return service.findAll(authentication, pageable, locationId);
    }

    @GetMapping("/{id}")
    public CouponDto findById(Authentication authentication, @PathVariable UUID id) {
        return service.findById(authentication, id);
    }

    @PostMapping
    public ResponseEntity<CouponDto> create(Authentication authentication, @RequestBody CouponDto dto) {
        CouponDto created = service.create(authentication, dto);
        return ResponseEntity.created(URI.create("/api/v1/manager/coupons/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public CouponDto update(Authentication authentication, @PathVariable UUID id, @RequestBody CouponDto dto) {
        return service.update(authentication, id, dto);
    }

    @PatchMapping("/{id}/status")
    public CouponDto updateStatus(Authentication authentication, @PathVariable UUID id, @RequestBody CouponStatusRequest request) {
        return service.updateStatus(authentication, id, request);
    }
}
