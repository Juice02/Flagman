package com.devjuice.flagman.repository;

import com.devjuice.flagman.entity.FeatureFlagConfig;
import com.devjuice.flagman.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleRepository extends JpaRepository<Rule,Integer> {

    void deleteByFeatureFlagConfigIn(List<FeatureFlagConfig> configs);

}
