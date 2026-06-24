package ca.umika.api.common.web;

import java.time.Instant;

public record ApiResult<T>(
        boolean success,
        T data,
        ApiErrorResponse error,
        Instant timestamp
) {
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(true, data, null, Instant.now());
    }

    public static <T> ApiResult<T> fail(ApiErrorResponse error) {
        return new ApiResult<>(false, null, error, Instant.now());
    }
}
