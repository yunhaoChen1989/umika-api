package ca.umika.api.payment;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/stripe")
@Tag(name = "StripePayment")
public class StripePaymentController {

    private final StripePaymentService service;

    public StripePaymentController(StripePaymentService service) {
        this.service = service;
    }

    @PostMapping("/payment-intent")
    public StripePaymentIntentResponse createPaymentIntent(
            Authentication authentication,
            @RequestBody StripePaymentIntentRequest request
    ) {
        return service.createPaymentIntent(authentication, request);
    }

    @PostMapping("/confirm")
    public StripePaymentStatusResponse confirm(
            Authentication authentication,
            @RequestBody StripePaymentConfirmRequest request
    ) {
        return service.confirm(authentication, request);
    }

    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public StripePaymentStatusResponse webhook(
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signature
    ) {
        return service.handleWebhook(payload, signature);
    }
}
