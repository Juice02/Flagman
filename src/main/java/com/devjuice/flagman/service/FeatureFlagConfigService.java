package com.devjuice.flagman.service;

import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.repository.FeatureFlagConfigRepository;
import com.devjuice.flagman.repository.FeatureFlagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeatureFlagConfigService {

    private final FeatureFlagConfigRepository featureFlagConfigRepository;

    public FeatureFlagConfigService(FeatureFlagConfigRepository featureFlagConfigRepository) { this.featureFlagConfigRepository=featureFlagConfigRepository;}

    public List<FeatureFlagConfig> getAllFeatureFlagConfig() {
        return featureFlagConfigRepository.findAll();
    }

}
