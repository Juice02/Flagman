package com.devjuice.flagman.repository;

import com.devjuice.flagman.entity.FeatureFlag;
import com.devjuice.flagman.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlag,Integer> {

    List<FeatureFlag> findByProject(Project project);

}
