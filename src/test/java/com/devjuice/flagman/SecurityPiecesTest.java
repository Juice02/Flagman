package com.devjuice.flagman;

import com.devjuice.flagman.entity.Environment;
import com.devjuice.flagman.sandbox.SandboxProperties;
import com.devjuice.flagman.sandbox.SandboxProperties.AllowedRule;
import com.devjuice.flagman.security.ClientIp;
import com.devjuice.flagman.security.RateLimiter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// The parts of access control that are plain logic. No database needed: ./mvnw test -Dtest=SecurityPiecesTest
class SecurityPiecesTest {

    @Test
    void clientIpTakesTheEntryAddedByTheTrustedProxy() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.5"); // the proxy
        request.addHeader("X-Forwarded-For", "1.1.1.1, 203.0.113.7"); // "1.1.1.1" was sent by the client and can be faked
        assertEquals("203.0.113.7", new ClientIp(1).of(request));
        assertEquals("10.0.0.5", new ClientIp(0).of(request)); // no proxy configured: the header is ignored
        assertEquals("1.1.1.1", new ClientIp(5).of(request)); // more hops than entries: the leftmost, never out of range
    }

    @Test
    void rateLimiterAllowsUpToTheLimitPerKey() {
        RateLimiter limiter = new RateLimiter();
        for (int i = 0; i < 3; i++) {
            assertTrue(limiter.allow("a", 3, Duration.ofMinutes(1)));
        }
        assertFalse(limiter.allow("a", 3, Duration.ofMinutes(1)));
        assertTrue(limiter.allow("b", 3, Duration.ofMinutes(1)), "keys are counted separately");
        assertTrue(limiter.exhausted("a", 3, Duration.ofMinutes(1)));
        assertFalse(limiter.exhausted("b", 3, Duration.ofMinutes(1)));
    }

    @Test
    void sandboxRulesMustMatchAPresetExactly() {
        SandboxProperties props = new SandboxProperties(Environment.DEV, List.of("dark-mode"),
                List.of(new AllowedRule("country", "IN", List.of("IN,GB", "US"))), 10, Duration.ofHours(24));
        assertTrue(props.allowsRule("country", "IN", "IN,GB"));
        assertFalse(props.allowsRule("country", "IN", "IN, GB"));
        assertFalse(props.allowsRule("country", "in", "US"));
        assertFalse(props.allowsRule("email", "IN", "US"));
        assertFalse(props.allowsRule("country", "CONTAINS", "US"));
    }
}
