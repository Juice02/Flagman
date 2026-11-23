package com.devjuice.flagman.controller;

import com.devjuice.flagman.dto.snapshot.response.Snapshot;
import com.devjuice.flagman.entity.Environment;
import com.devjuice.flagman.sandbox.SandboxService;
import com.devjuice.flagman.security.ClientIp;
import jakarta.servlet.http.HttpServletRequest;
import com.devjuice.flagman.stream.SnapshotService;
import com.devjuice.flagman.stream.StreamHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

// Lets clients evaluate flags themselves: a snapshot of every flag in an environment, once or as a live stream.
@RestController
public class SnapshotController {

    private final SnapshotService snapshots;
    private final StreamHub streams;
    private final SandboxService sandboxes;
    private final ClientIp clientIp;

    public SnapshotController(SnapshotService snapshots, StreamHub streams, SandboxService sandboxes, ClientIp clientIp) {
        this.snapshots = snapshots;
        this.streams = streams;
        this.sandboxes = sandboxes;
        this.clientIp = clientIp;
    }

    @GetMapping("/projects/{projectId}/environments/{environment}/snapshot")
    public Snapshot snapshot(@PathVariable Integer projectId, @PathVariable Environment environment) {
        return snapshots.build(projectId, environment);
    }

    @GetMapping(path = "/projects/{projectId}/environments/{environment}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Integer projectId, @PathVariable Environment environment,
                             HttpServletRequest request) {
        SseEmitter emitter = streams.subscribe(projectId, environment, clientIp.of(request));
        sandboxes.touch(projectId); // a visitor watching their sandbox keeps it from being cleaned up
        return emitter;
    }
}
