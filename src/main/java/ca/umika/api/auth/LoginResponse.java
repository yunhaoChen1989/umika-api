package ca.umika.api.auth;

public record LoginResponse(String accessToken, String tokenType) {
    public LoginResponse(String accessToken) {
        this(accessToken, "Bearer");
    }
}
