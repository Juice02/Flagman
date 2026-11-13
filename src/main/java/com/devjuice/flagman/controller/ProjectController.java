package com.devjuice.flagman.controller;

import com.devjuice.flagman.dto.project.request.CreateProjectRequest;
import com.devjuice.flagman.dto.project.response.ProjectResponse;
import com.devjuice.flagman.service.ProjectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects")
    public List<ProjectResponse> getProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/projects/{projectId}")
    public ProjectResponse getProject(@PathVariable Integer projectId) {
        return projectService.getProjectById(projectId);
    }

    @PostMapping("/projects")
    public ProjectResponse createProject(@RequestBody CreateProjectRequest request) {
        return projectService.createProject(request);
    }

}
