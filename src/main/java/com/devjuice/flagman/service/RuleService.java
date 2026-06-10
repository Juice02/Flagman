package com.devjuice.flagman.service;


import com.devjuice.flagman.entity.Rule;
import com.devjuice.flagman.repository.RuleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleService {

    private final RuleRepository ruleRepository;

    public RuleService(RuleRepository ruleRepository) { this.ruleRepository=ruleRepository; }

    public List<Rule> getAllRules() {
        return ruleRepository.findAll();
    }

}
