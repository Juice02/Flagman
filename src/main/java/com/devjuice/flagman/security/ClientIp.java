package com.devjuice.flagman.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// The visitor's IP address, used for rate limits. Behind a proxy (e.g. Render) the socket address is the
// proxy's, and the visitor's address is in X-Forwarded-For. Each proxy appends the address it saw, so with
// N trusted proxies the visitor is the Nth entry from the right; entries further left are whatever the
// client sent and can be faked. N comes from FLAGMAN_TRUSTED_PROXIES (0 = no proxy, use the socket address).
@Component
public class ClientIp {

    private final int trustedProxies;

    public ClientIp(@Value("${flagman.trusted-proxies}") int trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    public String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (trustedProxies == 0 || forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        String[] hops = forwarded.split(",");
        return hops[Math.max(0, hops.length - trustedProxies)].trim();
    }
}
