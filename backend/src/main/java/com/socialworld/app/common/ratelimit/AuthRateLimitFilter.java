package com.socialworld.app.common.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialworld.app.common.exception.ApiErrorResponse;
import com.socialworld.app.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Throttles the unauthenticated auth endpoints (login/register/refresh) per
 * client IP to slow down credential stuffing and brute-force attempts.
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    static final int MAX_REQUESTS = 15;
    static final int WINDOW_SECONDS = 60;

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String key = "auth:" + clientIp(request);
        if (!rateLimiter.tryAcquire(key, MAX_REQUESTS, WINDOW_SECONDS)) {
            response.setStatus(ErrorCode.RATE_LIMITED.getStatus().value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(),
                    ApiErrorResponse.of(ErrorCode.RATE_LIMITED, ErrorCode.RATE_LIMITED.getDefaultMessage()));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        // Behind a reverse proxy the first X-Forwarded-For entry is the client.
        // Trustworthy only when the proxy strips client-supplied values; direct
        // exposure falls back to the socket address.
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
