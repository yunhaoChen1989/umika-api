package ca.umika.api.user;

import java.time.LocalDate;
import java.util.UUID;

public record CurrentAccountProfileDto(
        UUID id,
        String email,
        String phone,
        String firstName,
        String lastName,
        LocalDate birthday,
        String preferredLanguage,
        Integer loyaltyPoints,
        String referralCode
) {
}
