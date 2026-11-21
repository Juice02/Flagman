package com.devjuice.flagman.stream;

import com.devjuice.flagman.dto.snapshot.response.FlagState;
import com.devjuice.flagman.dto.snapshot.response.Snapshot;
import com.devjuice.flagman.entity.Environment;
import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.entity.Project;
import com.devjuice.flagman.entity.Rule;
import com.devjuice.flagman.repository.FeatureFlagConfigRepository;
import com.devjuice.flagman.repository.ProjectRepository;
import com.devjuice.flagman.repository.RuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SnapshotService {

    private final ProjectRepository projectRepository;
    private final FeatureFlagConfigRepository configRepository;
    private final RuleRepository ruleRepository;

    public SnapshotService(ProjectRepository projectRepository, FeatureFlagConfigRepository configRepository,
                           RuleRepository ruleRepository) {
        this.projectRepository = projectRepository;
        this.configRepository = configRepository;
        this.ruleRepository = ruleRepository;
    }

    // Three queries whatever the number of flags. REQUIRES_NEW because StreamHub calls this after
    // another transaction has committed, when that transaction can no longer be used for reads.
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Snapshot build(int projectId, Environment environment) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project " + projectId + " not found"));
        List<FeatureFlagConfig> configs = configRepository.findByFeatureFlagProjectAndEnvironment(project, environment);
        Map<Integer, List<Rule>> rulesByConfig = configs.isEmpty() ? Map.of()
                : ruleRepository.findByFeatureFlagConfigIn(configs).stream()
                        .collect(Collectors.groupingBy(r -> r.getFeatureFlagConfig().getConfigId()));
        List<FlagState> flags = configs.stream()
                .map(c -> FlagState.of(c, rulesByConfig.getOrDefault(c.getConfigId(), List.of())))
                .sorted(Comparator.comparing(FlagState::flagKey))
                .toList();
        return new Snapshot(projectId, environment, project.getRevision(), flags);
    }
}
