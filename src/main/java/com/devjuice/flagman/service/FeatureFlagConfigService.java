package com.devjuice.flagman.service;

import com.devjuice.flagman.entity.FeatureFlag;
import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.repository.FeatureFlagConfigRepository;
import com.devjuice.flagman.repository.FeatureFlagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeatureFlagConfigService {

    private final FeatureFlagConfigRepository featureFlagConfigRepository;
    private final FeatureFlagRepository featureFlagRepository;

    public FeatureFlagConfigService(FeatureFlagConfigRepository featureFlagConfigRepository,FeatureFlagRepository featureFlagRepository) {
        this.featureFlagConfigRepository=featureFlagConfigRepository;
        this.featureFlagRepository = featureFlagRepository;
    }

    public List<FeatureFlagConfig> getAllFeatureFlagConfig() {
        return featureFlagConfigRepository.findAll();
    }

    public List<FeatureFlagConfig> getFeatureFlagConfigById(Integer featureId) {
        FeatureFlag featureFlag = featureFlagRepository.findById(featureId).orElseThrow();
        return featureFlagConfigRepository.findByFeatureFlag(featureFlag);
    }

}
