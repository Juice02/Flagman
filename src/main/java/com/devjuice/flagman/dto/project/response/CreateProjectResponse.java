package com.devjuice.flagman.dto.project.response;

import java.time.LocalDateTime;

public class CreateProjectResponse {

    private Integer projectId;
    private String name;
    private LocalDateTime createdOn;

    public Integer getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }


}
