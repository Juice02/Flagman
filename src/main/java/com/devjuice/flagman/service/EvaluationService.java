package com.devjuice.flagman.service;

import com.devjuice.flagman.dto.evaluate.request.EvaluateRequest;
import com.devjuice.flagman.dto.evaluate.response.EvaluateResponse;
import com.devjuice.flagman.dto.snapshot.response.FlagState;
import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.repository.RuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

// POST /evaluate: loads one flag's config and rules, then runs the shared Evaluator.
@Service
public class EvaluationService {

    private final FeatureFlagConfigService featureFlagConfigService;
    private final RuleRepository ruleRepository;

    public EvaluationService(FeatureFlagConfigService featureFlagConfigService, RuleRepository ruleRepository) {
        this.featureFlagConfigService = featureFlagConfigService;
        this.ruleRepository = ruleRepository;
    }

    public EvaluateResponse evaluate(EvaluateRequest request) {
        if (request.getProjectId() == null || isBlank(request.getFlagKey())
                || request.getEnvironment() == null || isBlank(request.getUserId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "projectId, flagKey, environment and userId are required");
        }

        FeatureFlagConfig config = featureFlagConfigService.findConfig(
                request.getProjectId(), request.getFlagKey(), request.getEnvironment());
        Map<String, String> attributes = request.getAttributes() == null ? Map.of() : request.getAttributes();
        return Evaluator.evaluate(
                FlagState.of(config, ruleRepository.findByFeatureFlagConfig(config)), request.getUserId(), attributes);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
