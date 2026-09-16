package com.devjuice.flagman.dto.project.response;

import java.time.LocalDateTime;

public class ProjectResponse {

    private Integer projectId;
    private String name;
    private String createdOn;

    public Integer getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }


}
