package com.devjuice.flagman.service;

import com.devjuice.flagman.dto.project.request.CreateProjectRequest;
import com.devjuice.flagman.dto.project.response.ProjectResponse;
import com.devjuice.flagman.entity.Project;
import com.devjuice.flagman.repository.ProjectRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }


    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse createProject(CreateProjectRequest request) {

        Project project = new Project();
        project.setName(request.getName());
        project.setCreatedOn(LocalDateTime.now());
        Project savedProject = projectRepository.save(project);

        return toResponse(savedProject);
    }

    public ProjectResponse getProjectById(Integer projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project " + projectId + " not found"));

        return toResponse(project);
    }

    // Converts the database entity into the shape the API returns.
    private ProjectResponse toResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setProjectId(project.getProjectId());
        response.setName(project.getName());
        response.setCreatedOn(project.getCreatedOn());
        return response;
    }
}
