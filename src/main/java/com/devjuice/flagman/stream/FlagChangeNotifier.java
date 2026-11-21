package com.devjuice.flagman.stream;

import com.devjuice.flagman.repository.ProjectRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Every write to a flag, config or rule calls this inside its transaction.
@Component
public class FlagChangeNotifier {

    private final ProjectRepository projectRepository;
    private final ApplicationEventPublisher events;

    public FlagChangeNotifier(ProjectRepository projectRepository, ApplicationEventPublisher events) {
        this.projectRepository = projectRepository;
        this.events = events;
    }

    // MANDATORY: without a surrounding transaction the after-commit listener would never run.
    @Transactional(propagation = Propagation.MANDATORY)
    public void changed(int projectId) {
        projectRepository.bumpRevision(projectId);
        events.publishEvent(new FlagsChanged(projectId));
    }
}
