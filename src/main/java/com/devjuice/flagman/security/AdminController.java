package com.devjuice.flagman.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

// Admin key only (see AccessInterceptor). After deploying behind a proxy, compare clientIp with your
// own public IP to check that FLAGMAN_TRUSTED_PROXIES is right; if it shows a proxy address, rate
// limits would treat all visitors as one.
@RestController
public class AdminController {

    private final ClientIp clientIp;
    private final JdbcTemplate jdbc;

    public AdminController(ClientIp clientIp, JdbcTemplate jdbc) {
        this.clientIp = clientIp;
        this.jdbc = jdbc;
    }

    @GetMapping("/admin/client-ip")
    public Map<String, String> clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return Map.of("clientIp", clientIp.of(request), "remoteAddr", request.getRemoteAddr(),
                "xForwardedFor", forwarded == null ? "" : forwarded);
    }

    // New demo visitors per day (sandboxes created), newest first, last 90 days.
    @GetMapping("/admin/stats")
    public List<Map<String, Object>> stats() {
        return jdbc.queryForList("SELECT DATE_FORMAT(day, '%Y-%m-%d') AS day, visitors FROM demo_visits ORDER BY day DESC LIMIT 90");
    }
}
