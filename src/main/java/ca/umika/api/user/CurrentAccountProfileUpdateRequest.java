package ca.umika.api.user;

import java.time.LocalDate;

public record CurrentAccountProfileUpdateRequest(
        String phone,
        String firstName,
        String lastName,
        LocalDate birthday,
        String preferredLanguage
) {
}
