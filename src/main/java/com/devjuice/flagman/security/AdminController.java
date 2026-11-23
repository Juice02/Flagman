package com.devjuice.flagman.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Admin key only (see AccessInterceptor). After deploying behind a proxy, compare clientIp with your
// own public IP to check that FLAGMAN_TRUSTED_PROXIES is right; if it shows a proxy address, rate
// limits would treat all visitors as one.
@RestController
public class AdminController {

    private final ClientIp clientIp;

    public AdminController(ClientIp clientIp) {
        this.clientIp = clientIp;
    }

    @GetMapping("/admin/client-ip")
    public Map<String, String> clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return Map.of("clientIp", clientIp.of(request), "remoteAddr", request.getRemoteAddr(),
                "xForwardedFor", forwarded == null ? "" : forwarded);
    }
}
