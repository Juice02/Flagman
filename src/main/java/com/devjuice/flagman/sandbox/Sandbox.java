package com.devjuice.flagman.sandbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sandboxes")
public class Sandbox {

    @Id
    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "last_used_on")
    private LocalDateTime lastUsedOn;

    protected Sandbox() {
    }

    public Sandbox(Integer projectId, String tokenHash, LocalDateTime now) {
        this.projectId = projectId;
        this.tokenHash = tokenHash;
        this.createdOn = now;
        this.lastUsedOn = now;
    }

    public Integer getProjectId() {
        return projectId;
    }
}
