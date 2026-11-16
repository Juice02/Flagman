package com.devjuice.flagman.controller;

import com.devjuice.flagman.dto.config.request.UpdateConfigRequest;
import com.devjuice.flagman.dto.config.response.FlagConfigResponse;
import com.devjuice.flagman.entity.Environment;
import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.service.FeatureFlagConfigService;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/projects/{projectId}/flags/{flagKey}/configs/{environment}")
    public FlagConfigResponse getConfig(@PathVariable Integer projectId, @PathVariable String flagKey,
                                        @PathVariable Environment environment) {
        return featureFlagConfigService.getConfig(projectId, flagKey, environment);
    }

    @PatchMapping("/projects/{projectId}/flags/{flagKey}/configs/{environment}")
    public FlagConfigResponse updateConfig(@PathVariable Integer projectId, @PathVariable String flagKey,
                                           @PathVariable Environment environment,
                                           @RequestBody UpdateConfigRequest request) {
        return featureFlagConfigService.updateConfig(projectId, flagKey, environment, request);
    }

}
