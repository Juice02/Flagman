package com.devjuice.flagman.repository;

import com.devjuice.flagman.entity.FeatureFlag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag,Integer> {
}
