package com.erstorage.erstorageapi.demo;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

@Component
public class DemoRateLimiter {

    private static final Duration WINDOW = Duration.ofHours(1);

    private final Map<String, Deque<Instant>> startsByClient = new HashMap<>();
    private final DemoProperties demo;

    public DemoRateLimiter(DemoProperties demo) {
        this.demo = demo;
    }

    public boolean tryAcquire(String clientKey) {
        return tryAcquire(clientKey, Instant.now());
    }

    synchronized boolean tryAcquire(String clientKey, Instant now) {
        Deque<Instant> starts = startsByClient.computeIfAbsent(clientKey, key -> new ArrayDeque<>());
        dropOld(starts, now);

        if (starts.size() >= demo.maxStartsPerIpPerHour()) {
            return false;
        }
        starts.addLast(now);
        return true;
    }

    public synchronized void forgetOldEntries() {
        Instant now = Instant.now();
        startsByClient.values().forEach(starts -> dropOld(starts, now));
        startsByClient.values().removeIf(Deque::isEmpty);
    }

    private void dropOld(Deque<Instant> starts, Instant now) {
        Instant windowStart = now.minus(WINDOW);
        while (!starts.isEmpty() && !starts.peekFirst().isAfter(windowStart)) {
            starts.pollFirst();
        }
    }
}