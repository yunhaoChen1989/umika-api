package ca.umika.api.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        long startedAt = System.currentTimeMillis();
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            logRequest(request, response, durationMs);
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return requestId.trim();
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String fullPath = query == null || query.isBlank() ? path : path + "?" + query;
        int status = response.getStatus();
        String user = resolveUser(request);

        if (status >= 500) {
            log.error("request completed method={} path={} status={} durationMs={} user={} remote={}",
                    method, fullPath, status, durationMs, user, request.getRemoteAddr());
            return;
        }
        if (status >= 400) {
            log.warn("request completed method={} path={} status={} durationMs={} user={} remote={}",
                    method, fullPath, status, durationMs, user, request.getRemoteAddr());
            return;
        }
        log.info("request completed method={} path={} status={} durationMs={} user={} remote={}",
                method, fullPath, status, durationMs, user, request.getRemoteAddr());
    }

    private String resolveUser(HttpServletRequest request) {
        Principal principal = request.getUserPrincipal();
        return principal == null || principal.getName() == null ? "anonymous" : principal.getName();
    }
}
