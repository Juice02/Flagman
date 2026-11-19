package com.devjuice.flagman.entity;

import jakarta.persistence.*;

@Entity
@Table(name="rules")

public class Rule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="rule_id")
    private Integer ruleId;

    @ManyToOne
    @JoinColumn(name="config_id")
    private FeatureFlagConfig featureFlagConfig;

    @Column(name="attribute")
    private String attribute;

    @Column(name="operator")
    private String operator;

    @Column(name="value")
    private String value;

    public Integer getRuleId() {
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

    public void setFeatureFlagConfig(FeatureFlagConfig featureFlagConfig) {
        this.featureFlagConfig = featureFlagConfig;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
