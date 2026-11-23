package com.devjuice.flagman.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// Fixed-window counters per key (e.g. "write:203.0.113.7").
// shortcut: in memory, so limits reset on restart and only work with one FlagMan instance; move to a shared store before scaling out.
@Component
public class RateLimiter {

    private static final Duration LONGEST_WINDOW = Duration.ofHours(1);

    private record Window(long start, AtomicInteger count) {
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    // Counts this call and says whether it is within the limit.
    public boolean allow(String key, int limit, Duration window) {
        return current(key, window).count().incrementAndGet() <= limit;
    }

    // Whether the limit is already used up, without counting this call.
    public boolean exhausted(String key, int limit, Duration window) {
        return current(key, window).count().get() >= limit;
    }

    private Window current(String key, Duration window) {
        long now = System.currentTimeMillis();
        return windows.compute(key, (k, w) ->
                w == null || now - w.start() >= window.toMillis() ? new Window(now, new AtomicInteger()) : w);
    }

    @Scheduled(fixedRate = 600_000)
    public void forgetOldWindows() {
        long cutoff = System.currentTimeMillis() - LONGEST_WINDOW.toMillis();
        windows.values().removeIf(w -> w.start() < cutoff);
    }
}
