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

    public int getProjectId() {
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

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }
}
