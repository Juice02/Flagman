package com.devjuice.flagman.controller;

import com.devjuice.flagman.sandbox.Sandbox;
import com.devjuice.flagman.sandbox.SandboxProperties;
import com.devjuice.flagman.security.AccessInterceptor;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.server.ResponseStatusException;

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

    private final SandboxProperties sandboxProperties;

    public RuleController(RuleService ruleService, SandboxProperties sandboxProperties) {
        this.ruleService = ruleService;
        this.sandboxProperties = sandboxProperties;
    }

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
                                   @PathVariable Environment environment, @RequestBody CreateRuleRequest request,
                                   @RequestAttribute(name = AccessInterceptor.SANDBOX, required = false) Sandbox sandbox) {
        // A sandbox token may only add the preset rules (AccessInterceptor already checked project, environment and flag).
        if (sandbox != null && !sandboxProperties.allowsRule(request.getAttribute(), request.getOperator(), request.getValue())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the preset rules can be added to a sandbox.");
        }
        return ruleService.createRule(projectId, flagKey, environment, request);
    }

    @DeleteMapping("/projects/{projectId}/flags/{flagKey}/configs/{environment}/rules/{ruleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable Integer projectId, @PathVariable String flagKey,
                           @PathVariable Environment environment, @PathVariable Integer ruleId) {
        ruleService.deleteRule(projectId, flagKey, environment, ruleId);
    }

}
