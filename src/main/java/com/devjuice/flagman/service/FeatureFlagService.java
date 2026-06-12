package com.devjuice.flagman.service;

import com.devjuice.flagman.entity.FeatureFlag;
import com.devjuice.flagman.entity.Project;
import com.devjuice.flagman.repository.FeatureFlagRepository;
import com.devjuice.flagman.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeatureFlagService {
    private final FeatureFlagRepository featureFlagRepository;
    private final ProjectRepository projectRepository;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository,ProjectRepository projectRepository) {
        this.featureFlagRepository=featureFlagRepository;
        this.projectRepository=projectRepository;
    }

    public List<FeatureFlag> getAllFeatureFlags() {
        return featureFlagRepository.findAll();
    }

    public List<FeatureFlag> getFlagsForProject(Integer projectId) {
        Project project = projectRepository.findById(projectId).orElseThrow();

        return featureFlagRepository.findByProject(project);

    }

}
