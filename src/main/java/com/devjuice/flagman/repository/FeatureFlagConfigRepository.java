package com.devjuice.flagman.repository;

import com.devjuice.flagman.entity.FeatureFlag;
import com.devjuice.flagman.entity.FeatureFlagConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeatureFlagConfigRepository extends JpaRepository<FeatureFlagConfig,Integer> {

    List<FeatureFlagConfig> findByFeatureFlag(FeatureFlag featureFlag);

}
