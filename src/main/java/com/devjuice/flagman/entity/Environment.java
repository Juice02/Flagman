package com.devjuice.flagman.entity;

/**
 * The fixed set of environments a flag can be configured for.
 * Stored in the database as the name (e.g. "PROD"), see FeatureFlagConfig.environment.
 */
public enum Environment {
    DEV,
    STAGING,
    PREPROD,
    PROD
}
