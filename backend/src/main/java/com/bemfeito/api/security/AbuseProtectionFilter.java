package com.bemfeito.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded, per-instance protection. The trusted proxy/container resolves remoteAddr. */
public final class AbuseProtectionFilter extends OncePerRequestFilter {
    private record Bucket(long start, int count) {}
    private final Map<String, Bucket> buckets = new HashMap<>();
    private final Clock clock;
    private final SecurityJson json;
    private final boolean enabled;
    public AbuseProtectionFilter(SecurityJson json, boolean enabled) { this(json, enabled, Clock.systemUTC()); }
    public AbuseProtectionFilter(SecurityJson json, boolean enabled, Clock clock) {
        this.json = json; this.enabled = enabled; this.clock = clock;
    }
    private synchronized boolean allow(String key, int limit, long window) {
        long now = clock.millis();
        Bucket previous = buckets.get(key);
        if (previous == null || now - previous.start() >= window) {
            if (buckets.size() >= 10000) {
                buckets.entrySet().removeIf(e -> now - e.getValue().start() >= 600000);
                if (buckets.size() >= 10000 && previous == null) return false;
            }
            buckets.put(key, new Bucket(now, 1));
            return true;
        }
        if (previous.count() >= limit) return false;
        buckets.put(key, new Bucket(previous.start(), previous.count() + 1));
        return true;
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (!enabled) { chain.doFilter(request, response); return; }
        String path = request.getServletPath();
        if (path.isEmpty()) path = request.getRequestURI();
        String group = null; int limit = 0; long window = 60000;
        if ("POST".equals(request.getMethod())) {
            if (path.equals("/api/auth/login")) { group = "login"; limit = 10; }
            else if (path.equals("/api/registration-requests") || path.equals("/api/access-requests")) {
                group = "requests"; limit = 10; window = 600000;
            } else if (path.startsWith("/api/account-invitations/")) { group = "invitation"; limit = 20; }
        } else if ("GET".equals(request.getMethod()) && path.equals("/api/auth/csrf")) {
            group = "csrf"; limit = 60;
        }
        if (group != null && !allow(group + ":" + request.getRemoteAddr(), limit, window)) {
            response.setHeader("Retry-After", Long.toString(window / 1000));
            json.error(response, 429, "Muitas tentativas. Aguarde antes de tentar novamente.");
            return;
        }
        chain.doFilter(request, response);
    }
}
