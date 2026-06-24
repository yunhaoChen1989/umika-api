package ca.umika.api.common.web;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        String path,
        String timestamp
) {
}
