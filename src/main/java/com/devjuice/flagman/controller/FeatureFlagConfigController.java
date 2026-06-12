package com.devjuice.flagman.controller;

import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.repository.FeatureFlagConfigRepository;
import com.devjuice.flagman.service.FeatureFlagConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FeatureFlagConfigController {

    private final FeatureFlagConfigService featureFlagConfigService;

    public FeatureFlagConfigController(FeatureFlagConfigService featureFlagConfigService) { this.featureFlagConfigService = featureFlagConfigService; }

    @GetMapping("/featureFlagConfigs")
    public List<FeatureFlagConfig> getFeatureFlagConfigs() {
        return featureFlagConfigService.getAllFeatureFlagConfig();
    }

    @GetMapping("/flags/{featureId}/configs")
    public List<FeatureFlagConfig> getFeatureFlagConfigById(@PathVariable Integer featureId) {
        return featureFlagConfigService.getFeatureFlagConfigById(featureId);
    }

}
