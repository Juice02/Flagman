package com.devjuice.flagman.service;

import com.devjuice.flagman.dto.evaluate.request.EvaluateRequest;
import com.devjuice.flagman.dto.evaluate.response.EvaluateResponse;
import com.devjuice.flagman.dto.evaluate.response.EvaluationReason;
import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.entity.Rule;
import com.devjuice.flagman.repository.RuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.CRC32;

@Service
public class EvaluationService {

    private final FeatureFlagConfigService featureFlagConfigService;
    private final RuleRepository ruleRepository;

    public EvaluationService(FeatureFlagConfigService featureFlagConfigService, RuleRepository ruleRepository) {
        this.featureFlagConfigService = featureFlagConfigService;
        this.ruleRepository = ruleRepository;
    }

    // Order: flag disabled -> rules (all must match) -> percentage rollout among the users who matched.
    public EvaluateResponse evaluate(EvaluateRequest request) {
        if (request.getProjectId() == null || isBlank(request.getFlagKey())
                || request.getEnvironment() == null || isBlank(request.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "projectId, flagKey, environment and userId are required");
        }

        FeatureFlagConfig config = featureFlagConfigService.findConfig(
                request.getProjectId(), request.getFlagKey(), request.getEnvironment());

        if (!Boolean.TRUE.equals(config.getEnabled())) {
            return new EvaluateResponse(false, EvaluationReason.FLAG_DISABLED);
        }

        Map<String, String> attributes = request.getAttributes() == null ? Map.of() : request.getAttributes();
        for (Rule rule : ruleRepository.findByFeatureFlagConfig(config)) {
            if (!matches(rule, attributes.get(rule.getAttribute()))) {
                return new EvaluateResponse(false, EvaluationReason.RULES_NOT_MATCHED);
            }
        }

        int rollout = config.getRolloutPercent() == null ? 0 : config.getRolloutPercent();
        boolean inRollout = bucket(request.getFlagKey(), request.getUserId()) < rollout;
        return new EvaluateResponse(inRollout, inRollout ? EvaluationReason.IN_ROLLOUT : EvaluationReason.OUT_OF_ROLLOUT);
    }

    // The same user and flag always land in the same 0-99 bucket; the flag key keeps buckets independent per flag.
    private int bucket(String flagKey, String userId) {
        CRC32 crc = new CRC32();
        crc.update((flagKey + ":" + userId).getBytes(StandardCharsets.UTF_8));
        return (int) (crc.getValue() % 100);
    }

    // Fails closed: a missing attribute or an unknown operator never matches.
    private boolean matches(Rule rule, String actual) {
        String expected = rule.getValue();
        if (actual == null || expected == null || rule.getOperator() == null) {
            return false;
        }
        return switch (rule.getOperator().toUpperCase()) {
            case "EQUALS" -> actual.equals(expected);
            case "NOT_EQUALS" -> !actual.equals(expected);
            case "IN" -> Arrays.stream(expected.split(",")).map(String::trim).anyMatch(actual::equals);
            case "CONTAINS" -> actual.contains(expected);
            default -> false;
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

}
