package com.devjuice.flagman.dto.config.request;

public class UpdateConfigRequest {

    private Boolean enabled;
    private Integer rolloutPercentage;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getRolloutPercentage() {
        return rolloutPercentage;
    }

    public void setRolloutPercentage(Integer rolloutPercentage) {
        this.rolloutPercentage = rolloutPercentage;
    }
}
