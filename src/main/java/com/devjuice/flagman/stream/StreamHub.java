package com.devjuice.flagman.stream;

import com.devjuice.flagman.dto.snapshot.response.Snapshot;
import com.devjuice.flagman.entity.Environment;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Open Server-Sent Event streams, grouped by project and environment. Each gets the current snapshot
// on connect and a new one after every committed change to that project.
// shortcut: in-memory, so it only works with one FlagMan instance; add a shared channel (e.g. Redis pub/sub) before running two.
@Component
public class StreamHub {

    private static final Logger log = LoggerFactory.getLogger(StreamHub.class);

    private record Key(int projectId, Environment environment) {
    }

    // Each open stream holds memory on a small (512 MB) server, so cap them per IP and in total.
    // A closed stream frees its slot when the next ping or the one after fails (within about 50 s).
    private static final int MAX_TOTAL = 500;
    private final int maxPerIp;

    private final Map<Key, Set<SseEmitter>> streams = new ConcurrentHashMap<>();
    private final Map<SseEmitter, String> owners = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> openPerIp = new ConcurrentHashMap<>();
    private final SnapshotService snapshots;

    public StreamHub(SnapshotService snapshots, @Value("${flagman.max-streams-per-ip}") int maxPerIp) {
        this.snapshots = snapshots;
        this.maxPerIp = maxPerIp;
    }

    public SseEmitter subscribe(int projectId, Environment environment, String ip) {
        Snapshot first = snapshots.build(projectId, environment); // 404 for an unknown project, before the stream opens
        if (owners.size() >= MAX_TOTAL) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The demo is busy. Try again later.");
        }
        AtomicInteger mine = openPerIp.computeIfAbsent(ip, k -> new AtomicInteger());
        if (mine.incrementAndGet() > maxPerIp) {
            mine.decrementAndGet();
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many open connections from this address.");
        }
        Key key = new Key(projectId, environment);
        SseEmitter emitter = new SseEmitter(0L); // no timeout; the client's EventSource reconnects if it drops
        owners.put(emitter, ip);
        streams.compute(key, (k, set) -> {
            Set<SseEmitter> emitters = set == null ? ConcurrentHashMap.newKeySet() : set;
            emitters.add(emitter);
            return emitters;
        });
        emitter.onCompletion(() -> remove(key, emitter));
        emitter.onTimeout(() -> remove(key, emitter));
        emitter.onError(e -> remove(key, emitter));
        send(key, emitter, SseEmitter.event().name("snapshot").id(Long.toString(first.revision())).data(first));
        return emitter;
    }

    // After commit, so a stream never shows a change that was rolled back. One snapshot per environment, sent to every stream.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChange(FlagsChanged event) {
        streams.forEach((key, emitters) -> {
            if (key.projectId() != event.projectId()) {
                return;
            }
            try {
                Snapshot snapshot = snapshots.build(key.projectId(), key.environment());
                emitters.forEach(e -> send(key, e, SseEmitter.event().name("snapshot")
                        .id(Long.toString(snapshot.revision())).data(snapshot)));
            } catch (RuntimeException e) {
                // The write already committed; a failed broadcast must not turn its response into an error.
                log.warn("Could not send snapshot for project {} {}", key.projectId(), key.environment(), e);
            }
        });
    }

    // A comment line every 25 s keeps proxies from closing idle streams and finds clients that left.
    @Scheduled(fixedRate = 25_000)
    public void ping() {
        streams.forEach((key, emitters) -> emitters.forEach(e -> send(key, e, SseEmitter.event().comment("ping"))));
    }

    // Closes every stream of a project that is being deleted (an idle sandbox).
    public void closeProject(int projectId) {
        streams.forEach((key, emitters) -> {
            if (key.projectId() == projectId) {
                emitters.forEach(SseEmitter::complete);
            }
        });
    }

    // Close streams on shutdown, or graceful shutdown waits for them until it times out.
    @PreDestroy
    public void closeAll() {
        streams.values().forEach(emitters -> emitters.forEach(SseEmitter::complete));
    }

    private void send(Key key, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException | IllegalStateException e) {
            remove(key, emitter); // the client went away
        }
    }

    private void remove(Key key, SseEmitter emitter) {
        String ip = owners.remove(emitter); // only the first removal of an emitter counts
        if (ip != null) {
            openPerIp.computeIfPresent(ip, (k, n) -> n.decrementAndGet() <= 0 ? null : n);
        }
        streams.computeIfPresent(key, (k, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
