package ca.umika.api.auth;

public record EmailVerificationResponse(
        boolean sent,
        int expiresInSeconds,
        int resendAfterSeconds
) {
}
