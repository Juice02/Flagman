package com.devjuice.flagman.entity;

import jakarta.persistence.*;

@Entity
@Table(name="rules")

public class Rule {

    @Id
    @Column(name="rule_id")
    private int ruleId;

    @ManyToOne
    @JoinColumn(name="config_id")
    private FeatureFlagConfig featureFlagConfig;

    @Column(name="attribute")
    private String attribute;

    @Column(name="operator")
    private String operator;

    @Column(name="value")
    private String value;

    public int getRuleId() {
        return ruleId;
    }

    public FeatureFlagConfig getFeatureFlagConfig() {
        return featureFlagConfig;
    }

    public String getAttribute() {
        return attribute;
    }

    public String getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

}
