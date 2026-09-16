package com.devjuice.flagman.entity;
import jakarta.persistence.*;

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
    private String createdOn;

    public int getProjectId() {
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
}
