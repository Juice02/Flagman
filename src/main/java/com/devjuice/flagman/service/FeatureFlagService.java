package com.devjuice.flagman.service;

import com.devjuice.flagman.entity.FeatureFlag;
import com.devjuice.flagman.repository.FeatureFlagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeatureFlagService {
    private final FeatureFlagRepository featureFlagRepository;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository) { this.featureFlagRepository=featureFlagRepository; }

    public List<FeatureFlag> getAllFeatureFlags() {
        return featureFlagRepository.findAll();
    }

}
