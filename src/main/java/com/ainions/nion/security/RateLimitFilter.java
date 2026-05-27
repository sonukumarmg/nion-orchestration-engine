package com.ainions.nion.security;

import com.ainions.nion.config.NionProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final NionProperties properties;

    public RateLimitFilter(NionProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String key = resolveKey(request);
        if (allowRequest(key)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.setStatus(429);
        response.getWriter().write("{" + "\"error\":\"rate_limit_exceeded\"}");
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.username();
        }
        return request.getRemoteAddr();
    }

    private boolean allowRequest(String key) {
        int limit = properties.rateLimit().requestsPerMinute();
        long currentMinute = Instant.now().getEpochSecond() / 60;
        Window window = windows.computeIfAbsent(key, ignored -> new Window(currentMinute));
        synchronized (window) {
            if (window.minuteBucket != currentMinute) {
                window.minuteBucket = currentMinute;
                window.counter.set(0);
            }
            return window.counter.incrementAndGet() <= limit;
        }
    }

    private static class Window {
        private long minuteBucket;
        private final AtomicInteger counter = new AtomicInteger(0);

        private Window(long minuteBucket) {
            this.minuteBucket = minuteBucket;
        }
    }
}
