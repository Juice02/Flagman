package com.devjuice.flagman.controller;

import com.devjuice.flagman.entity.Project;
import com.devjuice.flagman.repository.ProjectRepository;
import com.devjuice.flagman.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/projects")
    public List<Project> getProjects() {
        return projectService.getAllProjects();
    }

}
