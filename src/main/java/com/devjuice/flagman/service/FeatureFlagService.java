package com.devjuice.flagman.service;

import com.devjuice.flagman.dto.flag.request.CreateFlagRequest;
import com.devjuice.flagman.dto.flag.response.FlagResponse;
import com.devjuice.flagman.entity.Environment;
import com.devjuice.flagman.entity.FeatureFlag;
import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.entity.Project;
import com.devjuice.flagman.repository.FeatureFlagConfigRepository;
import com.devjuice.flagman.repository.FeatureFlagRepository;
import com.devjuice.flagman.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeatureFlagService {
    private final FeatureFlagRepository featureFlagRepository;
    private final ProjectRepository projectRepository;
    private final FeatureFlagConfigRepository featureFlagConfigRepository;

    public FeatureFlagService(FeatureFlagRepository featureFlagRepository, ProjectRepository projectRepository,
                              FeatureFlagConfigRepository featureFlagConfigRepository) {
        this.featureFlagRepository = featureFlagRepository;
        this.projectRepository = projectRepository;
        this.featureFlagConfigRepository = featureFlagConfigRepository;
    }

    public List<FeatureFlag> getAllFeatureFlags() {
        return featureFlagRepository.findAll();
    }

    public List<FlagResponse> getFlagsForProject(Integer projectId) {
        return featureFlagRepository.findByProject(findProject(projectId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // One transaction: if any config fails to save, the flag is rolled back too.
    @Transactional
    public FlagResponse createFlag(Integer projectId, CreateFlagRequest request) {
        Project project = findProject(projectId);

        // shortcut: two simultaneous requests can both pass this check; the DB unique constraint then returns a 500, fine until Phase 4 error handling.
        if (featureFlagRepository.existsByProjectAndFlagKey(project, request.getFlagKey())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Flag " + request.getFlagKey() + " already exists in project " + projectId);
        }

        LocalDateTime now = LocalDateTime.now();

        FeatureFlag flag = new FeatureFlag();
        flag.setProject(project);
        flag.setFlagKey(request.getFlagKey());
        flag.setFriendlyName(request.getFriendlyName());
        flag.setDescription(request.getDescription());
        flag.setCreatedOn(now);
        FeatureFlag savedFlag = featureFlagRepository.save(flag);

        // Every new flag starts with a config per environment, switched off.
        for (Environment environment : Environment.values()) {
            FeatureFlagConfig config = new FeatureFlagConfig();
            config.setFeatureFlag(savedFlag);
            config.setEnvironment(environment);
            config.setEnabled(false);
            config.setRolloutPercent(0);
            config.setCreatedOn(now);
            featureFlagConfigRepository.save(config);
        }

        return toResponse(savedFlag);
    }

    // Public so the config service (and later DELETE) can look up a flag the same way.
    public FeatureFlag findFlag(Integer projectId, String flagKey) {
        return featureFlagRepository.findByProjectAndFlagKey(findProject(projectId), flagKey)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Flag " + flagKey + " not found in project " + projectId));
    }

    private Project findProject(Integer projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project " + projectId + " not found"));
    }

    // Converts the database entity into the shape the API returns.
    private FlagResponse toResponse(FeatureFlag flag) {
        FlagResponse response = new FlagResponse();
        response.setProjectId(flag.getProject().getProjectId());
        response.setFlagKey(flag.getFlagKey());
        response.setFriendlyName(flag.getFriendlyName());
        response.setDescription(flag.getDescription());
        response.setCreatedOn(flag.getCreatedOn());
        return response;
    }

}
