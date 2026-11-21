package com.devjuice.flagman.entity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="projects")
public class Project {

    @Id
    @Column(name="project_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int projectId;

    @Column(name="name")
    private String name;

    @Column(name="created_on")
    private LocalDateTime createdOn;

    // Bumped by ProjectRepository.bumpRevision on every flag, config or rule change.
    @Column(name="revision", insertable = false, updatable = false)
    private long revision;

    public int getProjectId() {
        return projectId;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public long getRevision() {
        return revision;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }
}
