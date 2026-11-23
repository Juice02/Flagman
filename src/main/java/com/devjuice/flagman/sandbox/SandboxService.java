package com.devjuice.flagman.sandbox;

import com.devjuice.flagman.dto.flag.request.CreateFlagRequest;
import com.devjuice.flagman.dto.sandbox.response.SandboxResponse;
import com.devjuice.flagman.entity.Project;
import com.devjuice.flagman.repository.ProjectRepository;
import com.devjuice.flagman.service.FeatureFlagService;
import com.devjuice.flagman.stream.StreamHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

// Sandboxes: a private copy of the demo flags per visitor, changed with a token only that visitor holds.
@Service
public class SandboxService {

    private static final Logger log = LoggerFactory.getLogger(SandboxService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SandboxRepository sandboxes;
    private final ProjectRepository projects;
    private final FeatureFlagService flags;
    private final StreamHub streams;
    private final SandboxProperties props;
    private final TransactionTemplate tx;

    public SandboxService(SandboxRepository sandboxes, ProjectRepository projects, FeatureFlagService flags,
                          StreamHub streams, SandboxProperties props, PlatformTransactionManager txManager) {
        this.sandboxes = sandboxes;
        this.projects = projects;
        this.flags = flags;
        this.streams = streams;
        this.props = props;
        this.tx = new TransactionTemplate(txManager);
    }

    public SandboxResponse create() {
        return tx.execute(status -> {
            if (sandboxes.count() >= props.maxTotal()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The demo is busy. Try again later.");
            }
            byte[] secret = new byte[32];
            RANDOM.nextBytes(secret);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);

            Project project = new Project();
            project.setName("Sandbox " + token.substring(0, 8));
            project.setCreatedOn(LocalDateTime.now());
            int projectId = projects.save(project).getProjectId();
            for (String flagKey : props.flags()) {
                CreateFlagRequest request = new CreateFlagRequest();
                request.setFlagKey(flagKey);
                flags.createFlag(projectId, request); // every environment off, 0%, no rules
            }
            sandboxes.save(new Sandbox(projectId, hash(token), LocalDateTime.now()));
            return new SandboxResponse(projectId, props.environment(), token);
        });
    }

    public Optional<Sandbox> findByToken(String token) {
        return token.length() > 100 ? Optional.empty() : sandboxes.findByTokenHash(hash(token));
    }

    public boolean isSandbox(int projectId) {
        return sandboxes.existsById(projectId);
    }

    // Marks a sandbox as used (at most one write per hour), so the cleanup keeps it.
    public void touch(int projectId) {
        LocalDateTime now = LocalDateTime.now();
        tx.executeWithoutResult(status -> sandboxes.touch(projectId, now, now.minusHours(1)));
    }

    // Deletes sandboxes nobody has used for a while, with their flags, configs and rules.
    // Only rows in the sandboxes table are ever deleted here, never other projects.
    @Scheduled(fixedRate = 3_600_000, initialDelay = 60_000)
    public void deleteIdle() {
        for (Sandbox sandbox : sandboxes.findByLastUsedOnBefore(LocalDateTime.now().minus(props.idleTimeout()))) {
            int projectId = sandbox.getProjectId();
            try {
                streams.closeProject(projectId);
                tx.executeWithoutResult(status -> {
                    for (String flagKey : props.flags()) {
                        flags.deleteFlagIfPresent(projectId, flagKey);
                    }
                    sandboxes.deleteById(projectId);
                    projects.deleteById(projectId);
                });
            } catch (RuntimeException e) {
                log.warn("Could not delete idle sandbox {}", projectId, e);
            }
        }
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
