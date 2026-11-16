package com.devjuice.flagman.entity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="feature_flag_config")

public class FeatureFlagConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="config_id")
    private Integer configId;

    @ManyToOne
    @JoinColumn(name="flag_id")
    private FeatureFlag featureFlag;

    @Enumerated(EnumType.STRING)
    @Column(name="environment")
    private Environment environment;

    @Column(name="enabled")
    private Boolean enabled;

    @Column(name="rollout_percentage")
    private Integer rolloutPercent;

    @Column(name="created_on")
    private LocalDateTime createdOn;

    @Column(name="updated_on")
    private LocalDateTime updatedOn;

    public Integer getConfigId() {
        return configId;
    }

    public Integer getRolloutPercent() {
        return rolloutPercent;
    }

    public LocalDateTime getUpdatedOn() {
        return updatedOn;
    }

    public FeatureFlag getFeatureFlag() {
        return featureFlag;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setFeatureFlag(FeatureFlag featureFlag) {
        this.featureFlag = featureFlag;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public void setRolloutPercent(Integer rolloutPercent) {
        this.rolloutPercent = rolloutPercent;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public void setUpdatedOn(LocalDateTime updatedOn) {
        this.updatedOn = updatedOn;
    }
}
