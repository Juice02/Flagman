package com.devjuice.flagman.service;

import com.devjuice.flagman.dto.project.request.CreateProjectRequest;
import com.devjuice.flagman.dto.project.response.ProjectResponse;
import com.devjuice.flagman.entity.Project;
import com.devjuice.flagman.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }


    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public ProjectResponse createProject(CreateProjectRequest request) {

        Project project = new Project();
        project.setName(request.getName());
        project.setCreatedOn(LocalDateTime.now());
        Project savedProject = projectRepository.save(project);

        ProjectResponse response = new ProjectResponse();
        response.setProjectId(savedProject.getProjectId());
        response.setName(savedProject.getName());
        response.setCreatedOn(savedProject.getCreatedOn());

        return response;
    }

    // Response


}
