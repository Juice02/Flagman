package com.devjuice.flagman.repository;

import com.devjuice.flagman.entity.Rule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleRepository extends JpaRepository<Rule,Integer> {
}
