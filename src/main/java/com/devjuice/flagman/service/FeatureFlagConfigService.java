package com.devjuice.flagman.service;

import com.devjuice.flagman.stream.FlagChangeNotifier;
import com.devjuice.flagman.dto.config.request.UpdateConfigRequest;
import com.devjuice.flagman.dto.config.response.FlagConfigResponse;
import com.devjuice.flagman.entity.Environment;
import com.devjuice.flagman.entity.FeatureFlag;
import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.repository.FeatureFlagConfigRepository;
import com.devjuice.flagman.repository.FeatureFlagRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeatureFlagConfigService {

    private final FeatureFlagConfigRepository featureFlagConfigRepository;
    private final FeatureFlagRepository featureFlagRepository;
    private final FeatureFlagService featureFlagService;
    private final FlagChangeNotifier changes;

    public FeatureFlagConfigService(FeatureFlagConfigRepository featureFlagConfigRepository, FeatureFlagRepository featureFlagRepository,
                                    FeatureFlagService featureFlagService, FlagChangeNotifier changes) {
        this.featureFlagConfigRepository = featureFlagConfigRepository;
        this.featureFlagRepository = featureFlagRepository;
        this.featureFlagService = featureFlagService;
        this.changes = changes;
    }

    public List<FeatureFlagConfig> getAllFeatureFlagConfig() {
        return featureFlagConfigRepository.findAll();
    }

    public List<FeatureFlagConfig> getFeatureFlagConfigById(Integer featureId) {
        FeatureFlag featureFlag = featureFlagRepository.findById(featureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Flag " + featureId + " not found"));
        return featureFlagConfigRepository.findByFeatureFlag(featureFlag);
    }

    public FlagConfigResponse getConfig(Integer projectId, String flagKey, Environment environment) {
        return toResponse(findConfig(projectId, flagKey, environment));
    }

    @Transactional
    public FlagConfigResponse updateConfig(Integer projectId, String flagKey, Environment environment,
                                           UpdateConfigRequest request) {
        // PATCH: only the fields sent are changed; a missing field keeps its current value.
        Integer rollout = request.getRolloutPercentage();
        if (rollout != null && (rollout < 0 || rollout > 100)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rolloutPercentage must be between 0 and 100");
        }

        FeatureFlagConfig config = findConfig(projectId, flagKey, environment);
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        if (rollout != null) {
            config.setRolloutPercent(rollout);
        }
        config.setUpdatedOn(LocalDateTime.now());
        FeatureFlagConfig saved = featureFlagConfigRepository.save(config);
        changes.changed(projectId);
        return toResponse(saved);
    }

    // Public so the evaluation service can look up a config the same way.
    public FeatureFlagConfig findConfig(Integer projectId, String flagKey, Environment environment) {
        FeatureFlag flag = featureFlagService.findFlag(projectId, flagKey);
        return featureFlagConfigRepository.findByFeatureFlagAndEnvironment(flag, environment)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No " + environment + " config for flag " + flagKey));
    }

    // Converts the database entity into the shape the API returns.
    private FlagConfigResponse toResponse(FeatureFlagConfig config) {
        FlagConfigResponse response = new FlagConfigResponse();
        response.setEnvironment(config.getEnvironment());
        response.setEnabled(config.getEnabled());
        response.setRolloutPercentage(config.getRolloutPercent());
        return response;
    }

}
