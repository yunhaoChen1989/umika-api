package ca.umika.api.auth;

public record LoginResponse(String accessToken, String tokenType, String role) {
    public LoginResponse(String accessToken) {
        this(accessToken, "Bearer", null);
    }

    public LoginResponse(String accessToken, String role) {
        this(accessToken, "Bearer", role);
    }
}
