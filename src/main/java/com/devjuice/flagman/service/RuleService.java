package com.devjuice.flagman.service;


import com.devjuice.flagman.dto.rule.request.CreateRuleRequest;
import com.devjuice.flagman.dto.rule.response.RuleResponse;
import com.devjuice.flagman.entity.Environment;
import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.entity.Rule;
import com.devjuice.flagman.repository.RuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RuleService {

    private final RuleRepository ruleRepository;
    private final FeatureFlagConfigService featureFlagConfigService;

    public RuleService(RuleRepository ruleRepository, FeatureFlagConfigService featureFlagConfigService) {
        this.ruleRepository = ruleRepository;
        this.featureFlagConfigService = featureFlagConfigService;
    }

    public List<Rule> getAllRules() {
        return ruleRepository.findAll();
    }

    public List<RuleResponse> getRules(Integer projectId, String flagKey, Environment environment) {
        FeatureFlagConfig config = featureFlagConfigService.findConfig(projectId, flagKey, environment);
        return ruleRepository.findByFeatureFlagConfig(config).stream().map(this::toResponse).toList();
    }

    public RuleResponse createRule(Integer projectId, String flagKey, Environment environment, CreateRuleRequest request) {
        // Limits match the rules table columns (V3 migration), so a long value is a 400, not a database error.
        requireText("attribute", request.getAttribute(), 100);
        requireText("value", request.getValue(), 255);
        String operator = request.getOperator() == null ? null : request.getOperator().toUpperCase();
        if (!EvaluationService.OPERATORS.contains(operator)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "operator must be one of EQUALS, NOT_EQUALS, IN, CONTAINS");
        }

        Rule rule = new Rule();
        rule.setFeatureFlagConfig(featureFlagConfigService.findConfig(projectId, flagKey, environment));
        rule.setAttribute(request.getAttribute());
        rule.setOperator(operator);
        rule.setValue(request.getValue());
        return toResponse(ruleRepository.save(rule));
    }

    public void deleteRule(Integer projectId, String flagKey, Environment environment, Integer ruleId) {
        FeatureFlagConfig config = featureFlagConfigService.findConfig(projectId, flagKey, environment);
        // The rule must belong to this config, so a URL can't delete another flag's rule by ID.
        Rule rule = ruleRepository.findById(ruleId)
                .filter(r -> r.getFeatureFlagConfig().getConfigId().equals(config.getConfigId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Rule " + ruleId + " not found on " + environment + " config of flag " + flagKey));
        ruleRepository.delete(rule);
    }

    private void requireText(String field, String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " is required and must be at most " + maxLength + " characters");
        }
    }

    // Converts the database entity into the shape the API returns.
    private RuleResponse toResponse(Rule rule) {
        RuleResponse response = new RuleResponse();
        response.setRuleId(rule.getRuleId());
        response.setAttribute(rule.getAttribute());
        response.setOperator(rule.getOperator());
        response.setValue(rule.getValue());
        return response;
    }

}
