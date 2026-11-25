package com.devjuice.flagman;

import com.devjuice.flagman.dto.evaluate.request.EvaluateRequest;
import com.devjuice.flagman.dto.evaluate.response.EvaluateResponse;
import com.devjuice.flagman.entity.Environment;
import com.devjuice.flagman.entity.FeatureFlag;
import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.repository.RuleRepository;
import com.devjuice.flagman.service.EvaluationService;
import com.devjuice.flagman.service.FeatureFlagConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// POST /evaluate/flags: several flags for one user. No database needed: ./mvnw test -Dtest=EvaluateFlagsTest
class EvaluateFlagsTest {

    private final FeatureFlagConfigService configs = mock(FeatureFlagConfigService.class);
    private final RuleRepository rules = mock(RuleRepository.class);
    private final EvaluationService service = new EvaluationService(configs, rules);

    @Test
    void answersEveryFlagInTheOrderAsked() {
        when(configs.findConfig(eq(1), eq("dark-mode"), eq(Environment.DEV))).thenReturn(config("dark-mode", true, 100));
        when(configs.findConfig(eq(1), eq("hide-non-fiction"), eq(Environment.DEV))).thenReturn(config("hide-non-fiction", false, 100));
        when(rules.findByFeatureFlagConfig(any())).thenReturn(List.of());

        Map<String, EvaluateResponse> result = service.evaluateFlags(request(List.of("hide-non-fiction", "dark-mode")));

        assertEquals(List.of("hide-non-fiction", "dark-mode"), List.copyOf(result.keySet()));
        assertEquals("FLAG_DISABLED", result.get("hide-non-fiction").getReason().name());
        assertEquals("IN_ROLLOUT", result.get("dark-mode").getReason().name());
    }

    @Test
    void refusesMissingOrTooManyFlags() {
        assertThrows(ResponseStatusException.class, () -> service.evaluateFlags(request(null)));
        assertThrows(ResponseStatusException.class, () -> service.evaluateFlags(request(List.of())));
        assertThrows(ResponseStatusException.class, () -> service.evaluateFlags(request(List.of(" "))));
        assertThrows(ResponseStatusException.class, () -> service.evaluateFlags(request(Collections.nCopies(21, "dark-mode"))));
    }

    @Test
    void refusesOversizedUsers() {
        EvaluateRequest longId = request(List.of("dark-mode"));
        longId.setUserId("u".repeat(101));
        EvaluateRequest manyAttributes = request(List.of("dark-mode"));
        Map<String, String> attributes = new java.util.HashMap<>();
        for (int i = 0; i < 21; i++) {
            attributes.put("a" + i, "x");
        }
        manyAttributes.setAttributes(attributes);
        EvaluateRequest longValue = request(List.of("dark-mode"));
        longValue.setAttributes(Map.of("country", "x".repeat(256)));

        for (EvaluateRequest tooBig : List.of(longId, manyAttributes, longValue)) {
            ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.evaluateFlags(tooBig));
            assertEquals(400, e.getStatusCode().value());
        }
    }

    private static EvaluateRequest request(List<String> flagKeys) {
        EvaluateRequest request = new EvaluateRequest();
        request.setProjectId(1);
        request.setEnvironment(Environment.DEV);
        request.setUserId("ravi");
        request.setAttributes(Map.of("country", "IN"));
        request.setFlagKeys(flagKeys);
        return request;
    }

    private static FeatureFlagConfig config(String flagKey, boolean enabled, int rollout) {
        FeatureFlag flag = new FeatureFlag();
        flag.setFlagKey(flagKey);
        FeatureFlagConfig config = new FeatureFlagConfig();
        config.setFeatureFlag(flag);
        config.setEnabled(enabled);
        config.setRolloutPercent(rollout);
        return config;
    }
}
