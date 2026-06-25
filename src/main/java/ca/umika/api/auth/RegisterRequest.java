package ca.umika.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank
        @Email
        String email,

        String phone,

        @NotBlank
        @Size(min = 8)
        String password,

        String firstName,

        String lastName,

        String preferredLanguage,

        String referralCode
) {
}
