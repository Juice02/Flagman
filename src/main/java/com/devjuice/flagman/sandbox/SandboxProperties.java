package com.devjuice.flagman.sandbox;

import com.devjuice.flagman.entity.Environment;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

// What a sandbox contains and what its token may change; set in application.properties (flagman.sandbox.*).
@ConfigurationProperties("flagman.sandbox")
public record SandboxProperties(Environment environment, List<String> flags, List<AllowedRule> allowedRules,
                                int maxTotal, Duration idleTimeout) {

    // One preset: a fixed attribute and operator, and the values a visitor may pick.
    public record AllowedRule(String attribute, String operator, List<String> values) {
    }

    // Exact match: a sandbox token can only create rules that are in this list.
    public boolean allowsRule(String attribute, String operator, String value) {
        return allowedRules.stream().anyMatch(r -> r.attribute().equals(attribute)
                && r.operator().equals(operator) && r.values().contains(value));
    }
}
