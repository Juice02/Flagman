package com.devjuice.flagman.controller;

import com.devjuice.flagman.entity.Rule;
import com.devjuice.flagman.service.RuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) { this.ruleService = ruleService; }

    @GetMapping("/rules")
    public List<Rule> getAllRules () {
        return ruleService.getAllRules();
    }

}
