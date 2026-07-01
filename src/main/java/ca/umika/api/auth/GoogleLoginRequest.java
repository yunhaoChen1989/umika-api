package ca.umika.api.auth;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank String credential,
        String referralCode
) {
}
