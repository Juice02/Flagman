package com.devjuice.flagman.controller;

import com.devjuice.flagman.dto.rule.request.CreateRuleRequest;
import com.devjuice.flagman.dto.rule.response.RuleResponse;
import com.devjuice.flagman.entity.Environment;
import com.devjuice.flagman.entity.Rule;
import com.devjuice.flagman.service.RuleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) { this.ruleService = ruleService; }

    @GetMapping("/rules")
    public List<Rule> getAllRules () {
        return ruleService.getAllRules();
    }

    @GetMapping("/projects/{projectId}/flags/{flagKey}/configs/{environment}/rules")
    public List<RuleResponse> getRules(@PathVariable Integer projectId, @PathVariable String flagKey,
                                       @PathVariable Environment environment) {
        return ruleService.getRules(projectId, flagKey, environment);
    }

    @PostMapping("/projects/{projectId}/flags/{flagKey}/configs/{environment}/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public RuleResponse createRule(@PathVariable Integer projectId, @PathVariable String flagKey,
                                   @PathVariable Environment environment, @RequestBody CreateRuleRequest request) {
        return ruleService.createRule(projectId, flagKey, environment, request);
    }

    @DeleteMapping("/projects/{projectId}/flags/{flagKey}/configs/{environment}/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable Integer projectId, @PathVariable String flagKey,
                           @PathVariable Environment environment, @PathVariable Integer ruleId) {
        ruleService.deleteRule(projectId, flagKey, environment, ruleId);
    }

}
