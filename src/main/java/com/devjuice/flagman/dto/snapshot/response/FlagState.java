package com.devjuice.flagman.dto.snapshot.response;

import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.entity.Rule;

import java.util.List;

// Everything needed to evaluate one flag in one environment, without a database.
public record FlagState(String flagKey, boolean enabled, int rolloutPercentage, List<RuleState> rules) {

    public static FlagState of(FeatureFlagConfig config, List<Rule> rules) {
        return new FlagState(
                config.getFeatureFlag().getFlagKey(),
                Boolean.TRUE.equals(config.getEnabled()),
                config.getRolloutPercent() == null ? 0 : config.getRolloutPercent(),
                rules.stream().map(r -> new RuleState(r.getAttribute(), r.getOperator(), r.getValue())).toList());
    }
}
