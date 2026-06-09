package com.devjuice.flagman.controller;


import com.devjuice.flagman.entity.FeatureFlag;
import com.devjuice.flagman.entity.Project;
import com.devjuice.flagman.service.FeatureFlagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class FeatureFlagController {
    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) { this.featureFlagService = featureFlagService; }

    @GetMapping("/featureFlags")
    public List<FeatureFlag> getFeatureFlags() {
        return featureFlagService.getAllFeatureFlags();
    }
}
