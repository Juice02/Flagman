package com.devjuice.flagman.dto.config.response;

import com.devjuice.flagman.entity.Environment;

public class FlagConfigResponse {

    private Environment environment;
    private Boolean enabled;
    private Integer rolloutPercentage;

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

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
