package com.devjuice.flagman.repository;

import com.devjuice.flagman.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project,Integer> {

    // The project list leaves out visitors' sandboxes, which can number in the thousands.
    @Query("select p from Project p where p.projectId not in (select s.projectId from Sandbox s)")
    List<Project> findAllExceptSandboxes();

    // In the caller's transaction, so the new revision commits together with the change it counts.
    @Modifying
    @Query("update Project p set p.revision = p.revision + 1 where p.projectId = :projectId")
    void bumpRevision(@Param("projectId") int projectId);
}
