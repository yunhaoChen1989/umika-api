package ca.umika.api.reward;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/rewards")
@Tag(name = "CurrentUserRewards")
public class CurrentUserRewardsController {

    private final CurrentUserRewardsService service;

    public CurrentUserRewardsController(CurrentUserRewardsService service) {
        this.service = service;
    }

    @GetMapping
    public CurrentUserRewardsDto summary(
            Authentication authentication,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) String locationCode
    ) {
        return service.summary(authentication, locationId, locationCode);
    }

    @GetMapping("/transactions")
    public Page<CurrentUserRewardTransactionDto> transactions(Authentication authentication, Pageable pageable) {
        return service.transactions(authentication, pageable);
    }

    @GetMapping("/redemption-status")
    public CurrentUserRewardRedemptionStatusDto redemptionStatus(
            Authentication authentication,
            @RequestParam(required = false) UUID locationId,
            @RequestParam(required = false) String locationCode
    ) {
        return service.redemptionStatus(authentication, locationId, locationCode);
    }
}
