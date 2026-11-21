package com.devjuice.flagman.repository;

import com.devjuice.flagman.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project,Integer> {

    // In the caller's transaction, so the new revision commits together with the change it counts.
    @Modifying
    @Query("update Project p set p.revision = p.revision + 1 where p.projectId = :projectId")
    void bumpRevision(@Param("projectId") int projectId);
}
