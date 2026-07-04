package ca.umika.api.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiResult<Void>> handleNotFound(ResourceNotFoundException exception, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResult.fail(error));
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ApiResult<Void>> handleBadCredentials(BadCredentialsException exception, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                exception.getMessage(),
                request.getRequestURI(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResult.fail(error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResult<Void>> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Invalid request payload",
                request.getRequestURI(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail(error));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiResult<Void>> handleResponseStatus(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatusCode statusCode = exception.getStatusCode();
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String error = status == null ? "Error" : status.getReasonPhrase();
        if (statusCode.is5xxServerError()) {
            log.error("response status exception path={} status={} message={}",
                    request.getRequestURI(), statusCode.value(), exception.getReason(), exception);
        } else if (statusCode.is4xxClientError()) {
            log.warn("response status exception path={} status={} message={}",
                    request.getRequestURI(), statusCode.value(), exception.getReason());
        }
        ApiErrorResponse apiError = new ApiErrorResponse(
                statusCode.value(),
                error,
                resolveMessage(exception.getReason(), error),
                request.getRequestURI(),
                Instant.now().toString()
        );
        return ResponseEntity.status(statusCode).body(ApiResult.fail(apiError));
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiResult<Void>> handleBadRequest(Exception exception, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                resolveMessage(exception.getMessage(), "Invalid request"),
                request.getRequestURI(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResult.fail(error));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResult<Void>> handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        log.warn("data integrity violation path={} message={}", request.getRequestURI(), exception.getMostSpecificCause().getMessage());
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.CONFLICT.value(),
                HttpStatus.CONFLICT.getReasonPhrase(),
                "Request conflicts with existing data",
                request.getRequestURI(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResult.fail(error));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResult<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                resolveMessage(exception.getMessage(), "Request method is not supported"),
                request.getRequestURI(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(ApiResult.fail(error));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiResult<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase(),
                resolveMessage(exception.getMessage(), "Content type is not supported"),
                request.getRequestURI(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(ApiResult.fail(error));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResult<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("unexpected server error path={} message={}", request.getRequestURI(), exception.getMessage(), exception);
        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Unexpected server error",
                request.getRequestURI(),
                Instant.now().toString()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResult.fail(error));
    }

    private String resolveMessage(String message, String fallback) {
        return message == null || message.isBlank() ? fallback : message;
    }
}
