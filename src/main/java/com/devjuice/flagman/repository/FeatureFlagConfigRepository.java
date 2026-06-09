package com.devjuice.flagman.repository;

import com.devjuice.flagman.entity.FeatureFlagConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagConfigRepository extends JpaRepository<FeatureFlagConfig,Integer> {
}
