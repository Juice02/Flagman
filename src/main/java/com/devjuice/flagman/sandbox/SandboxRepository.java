package com.devjuice.flagman.sandbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SandboxRepository extends JpaRepository<Sandbox, Integer> {

    Optional<Sandbox> findByTokenHash(String tokenHash);

    List<Sandbox> findByLastUsedOnBefore(LocalDateTime cutoff);

    // Only writes when the stored time is older than `staleBefore`, so busy sandboxes don't cause a write per request.
    @Modifying
    @Query("update Sandbox s set s.lastUsedOn = :now where s.projectId = :projectId and s.lastUsedOn < :staleBefore")
    void touch(@Param("projectId") int projectId, @Param("now") LocalDateTime now,
               @Param("staleBefore") LocalDateTime staleBefore);
}
