package com.devjuice.flagman.service;

import com.devjuice.flagman.dto.evaluate.request.EvaluateRequest;
import com.devjuice.flagman.dto.evaluate.response.EvaluateResponse;
import com.devjuice.flagman.dto.snapshot.response.FlagState;
import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.repository.RuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// POST /evaluate and /evaluate/flags: load each flag's config and rules, then run the shared Evaluator.
@Service
public class EvaluationService {

    // Each flag costs two database reads; the cap keeps one request cheap.
    public static final int MAX_FLAGS = 20;

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

        checkSizes(request);
        FeatureFlagConfig config = featureFlagConfigService.findConfig(
                request.getProjectId(), request.getFlagKey(), request.getEnvironment());
        Map<String, String> attributes = request.getAttributes() == null ? Map.of() : request.getAttributes();
        return Evaluator.evaluate(
                FlagState.of(config, ruleRepository.findByFeatureFlagConfig(config)), request.getUserId(), attributes);
    }

    // Every listed flag for one user, in the order asked; what a browser or mobile app needs on load and after a change.
    public Map<String, EvaluateResponse> evaluateFlags(EvaluateRequest request) {
        List<String> flagKeys = request.getFlagKeys();
        if (request.getProjectId() == null || request.getEnvironment() == null || isBlank(request.getUserId())
                || flagKeys == null || flagKeys.isEmpty() || flagKeys.stream().anyMatch(this::isBlank)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "projectId, environment, userId and flagKeys are required");
        }
        if (flagKeys.size() > MAX_FLAGS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At most " + MAX_FLAGS + " flags per request");
        }
        Map<String, EvaluateResponse> results = new LinkedHashMap<>();
        for (String flagKey : flagKeys) {
            EvaluateRequest one = new EvaluateRequest();
            one.setProjectId(request.getProjectId());
            one.setEnvironment(request.getEnvironment());
            one.setUserId(request.getUserId());
            one.setAttributes(request.getAttributes());
            one.setFlagKey(flagKey);
            results.put(flagKey, evaluate(one));
        }
        return results;
    }

    // The same limits rules have, so a request can't make the server hold or compare huge values.
    private void checkSizes(EvaluateRequest request) {
        Map<String, String> attributes = request.getAttributes();
        boolean tooBig = request.getUserId().length() > 100 || (attributes != null && (attributes.size() > 20
                || attributes.entrySet().stream().anyMatch(a -> a.getKey() == null || a.getKey().length() > 100
                        || (a.getValue() != null && a.getValue().length() > 255))));
        if (tooBig) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "userId is up to 100 characters; at most 20 attributes, names up to 100 and values up to 255 characters");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
