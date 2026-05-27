package com.ainions.nion.security;

import com.ainions.nion.config.NionProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
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
        Bucket bucket = buckets.computeIfAbsent(key, this::newBucket);
        if (bucket.tryConsume(1)) {
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

    private Bucket newBucket(String key) {
        int limit = properties.rateLimit().requestsPerMinute();
        Refill refill = Refill.greedy(limit, Duration.ofMinutes(1));
        Bandwidth bandwidth = Bandwidth.classic(limit, refill);
        return Bucket.builder().addLimit(bandwidth).build();
    }
}
