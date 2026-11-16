package com.devjuice.flagman.controller;


import com.devjuice.flagman.dto.flag.request.CreateFlagRequest;
import com.devjuice.flagman.dto.flag.response.FlagResponse;
import com.devjuice.flagman.entity.FeatureFlag;
import com.devjuice.flagman.service.FeatureFlagService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FeatureFlagController {
    private final FeatureFlagService featureFlagService;

    public FeatureFlagController(FeatureFlagService featureFlagService) { this.featureFlagService = featureFlagService; }

    @GetMapping("/featureFlags")
    public List<FeatureFlag> getFeatureFlags() {
        return featureFlagService.getAllFeatureFlags();
    }

    @GetMapping("projects/{projectId}/flags")
    public List<FeatureFlag> getFlagsForProject(@PathVariable Integer projectId) {
        return featureFlagService.getFlagsForProject(projectId);
    }

    @PostMapping("/projects/{projectId}/flags")
    @ResponseStatus(HttpStatus.CREATED)
    public FlagResponse createFlag(@PathVariable Integer projectId, @RequestBody CreateFlagRequest request) {
        return featureFlagService.createFlag(projectId, request);
    }

}
