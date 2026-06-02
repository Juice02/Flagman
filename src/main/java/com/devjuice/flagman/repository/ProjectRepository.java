package com.devjuice.flagman.repository;

import com.devjuice.flagman.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project,Integer> {
    
}