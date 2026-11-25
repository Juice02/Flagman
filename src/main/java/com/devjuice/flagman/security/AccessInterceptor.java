package com.devjuice.flagman.security;

import com.devjuice.flagman.sandbox.Sandbox;
import com.devjuice.flagman.sandbox.SandboxProperties;
import com.devjuice.flagman.sandbox.SandboxService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Runs before every API request. Default deny for changes: a change needs the admin key, except the
// preset changes a sandbox token may make on its own sandbox. Reads stay open. Also applies rate limits.
// When FLAGMAN_ADMIN_KEY is not set (local development), changes are open as before.
@Component
public class AccessInterceptor implements HandlerInterceptor {

    // The request attribute RuleController reads to check a sandbox's rule against the presets.
    public static final String SANDBOX = "flagman.sandbox";

    private static final Logger log = LoggerFactory.getLogger(AccessInterceptor.class);
    private static final Duration MINUTE = Duration.ofMinutes(1);
    // Counted per flag evaluated, since each costs database reads: /evaluate is one, /evaluate/flags one per flag.
    private static final int EVALUATIONS_PER_MINUTE = 600;
    private static final String TOO_MANY_EVALUATIONS = "Too many requests. Try again in a minute.";

    // The only routes a sandbox token can use; everything else needs the admin key.
    private static final Set<String> SANDBOX_ROUTES = Set.of(
            "PATCH /projects/{projectId}/flags/{flagKey}/configs/{environment}",
            "POST /projects/{projectId}/flags/{flagKey}/configs/{environment}/rules",
            "DELETE /projects/{projectId}/flags/{flagKey}/configs/{environment}/rules/{ruleId}");

    private final byte[] adminKey;
    private final SandboxService sandboxes;
    private final SandboxProperties props;
    private final RateLimiter limiter;
    private final ClientIp clientIp;

    public AccessInterceptor(@Value("${flagman.admin-key}") String adminKey, SandboxService sandboxes,
                             SandboxProperties props, RateLimiter limiter, ClientIp clientIp) {
        if (!adminKey.isEmpty() && adminKey.length() < 32) {
            throw new IllegalStateException("FLAGMAN_ADMIN_KEY must be at least 32 characters");
        }
        if (adminKey.isEmpty()) {
            log.warn("FLAGMAN_ADMIN_KEY is not set: every change is allowed without a key. Set it before exposing FlagMan.");
        }
        this.adminKey = adminKey.getBytes(StandardCharsets.UTF_8);
        this.sandboxes = sandboxes;
        this.props = props;
        this.limiter = limiter;
        this.clientIp = clientIp;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        // The route Spring matched (e.g. /admin/stats), not the raw URI: "/admin;x=1/stats" or "/%61dmin/stats"
        // reach the same handler but don't start with "/admin/". Raw URI only when nothing matched (404).
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String path = pattern != null ? pattern.toString() : request.getRequestURI();
        String ip = clientIp.of(request);

        if (path.startsWith("/admin/")) {
            requireAdmin(request, ip);
            return true;
        }
        if (method.equals("GET") || method.equals("HEAD") || method.equals("OPTIONS")) {
            if (path.endsWith("/stream")) {
                limit("stream-open:" + ip, 30, MINUTE, "Too many requests. Try again in a minute."); // reconnects; concurrent streams are capped in StreamHub
            }
            return true;
        }
        if (method.equals("POST") && (path.equals("/evaluate") || path.equals("/evaluate/flags"))) {
            limit("evaluate:" + ip, EVALUATIONS_PER_MINUTE, MINUTE, TOO_MANY_EVALUATIONS); // a read, but it costs database queries
            return true;
        }
        if (method.equals("POST") && path.equals("/sandboxes")) {
            limit("sandbox:" + ip, 10, Duration.ofHours(1), "Too many new sandboxes from this address. Try again in an hour.");
            return true;
        }

        limit("write:" + ip, 60, MINUTE, "Too many changes. Try again in a minute.");
        if (request.getHeader("X-Admin-Key") != null || adminKey.length == 0) {
            requireAdmin(request, ip);
            return true;
        }
        String token = request.getHeader("X-Sandbox-Token");
        if (token == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This change needs the admin key.");
        }
        Sandbox sandbox = sandboxes.findByToken(token).filter(s -> sandboxMayChange(s, request))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "A sandbox token can only change its own demo flags with the preset options."));
        request.setAttribute(SANDBOX, sandbox);
        sandboxes.touch(sandbox.getProjectId());
        return true;
    }

    // preHandle already counted one evaluation for /evaluate/flags; count the request's other flags.
    public void countMoreEvaluations(HttpServletRequest request, int more) {
        String key = "evaluate:" + clientIp.of(request);
        for (int i = 0; i < more; i++) {
            limit(key, EVALUATIONS_PER_MINUTE, MINUTE, TOO_MANY_EVALUATIONS);
        }
    }

    private boolean sandboxMayChange(Sandbox sandbox, HttpServletRequest request) {
        String route = request.getMethod() + " " + request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        @SuppressWarnings("unchecked")
        Map<String, String> vars = Optional.ofNullable(
                (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE)).orElse(Map.of());
        return SANDBOX_ROUTES.contains(route)
                && String.valueOf(sandbox.getProjectId()).equals(vars.get("projectId"))
                && props.environment().name().equals(vars.get("environment"))
                && props.flags().contains(vars.get("flagKey"));
    }

    private void requireAdmin(HttpServletRequest request, String ip) {
        if (adminKey.length == 0) {
            return; // local development: no key configured
        }
        String failures = "admin-fail:" + ip;
        Duration lockout = Duration.ofMinutes(15);
        if (limiter.exhausted(failures, 10, lockout)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong admin keys. Try again later.");
        }
        String given = request.getHeader("X-Admin-Key");
        // Constant-time comparison, so response timing doesn't reveal how much of the key was right.
        if (given == null || !MessageDigest.isEqual(adminKey, given.getBytes(StandardCharsets.UTF_8))) {
            limiter.allow(failures, 10, lockout);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "This change needs the admin key.");
        }
    }

    private void limit(String key, int max, Duration window, String message) {
        if (!limiter.allow(key, max, window)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message);
        }
    }
}
