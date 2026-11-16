package com.devjuice.flagman.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="feature_flags")

public class FeatureFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="flag_id")
    private Integer flagId;

    @ManyToOne
    @JoinColumn(name="project_id")
    private Project project;

    @Column(name="flag_key")
    private String flagKey;

    @Column(name="friendly_name")
    private String friendlyName;

    @Column(name="description")
    private String description;

    @Column(name="created_on")
    private LocalDateTime createdOn;

    @Column(name="updated_on")
    private LocalDateTime updatedOn;


    public Integer getFlagId() {
        return flagId;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    public Project getProject() {
        return project;
    }

    public String getDescription() {
        return description;
    }

    public String getFlagKey() {
        return flagKey;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setFlagKey(String flagKey) {
        this.flagKey = flagKey;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

}
