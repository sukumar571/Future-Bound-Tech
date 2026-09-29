package com.futureboundtech.service.mentor;

import com.futureboundtech.config.FutureMentorProperties;
import com.futureboundtech.exception.RateLimitedException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A tiny in-memory fixed-window (sliding 60-second) limiter, keyed per student.
 * It exists to blunt abuse and cap outbound AI spend; it is intentionally simple
 * and process-local (a multi-instance deployment would front it with a shared
 * store, which is out of scope for this optional module).
 */
@Component
public class MentorRateLimiter {

    private static final long WINDOW_MILLIS = 60_000L;

    private final FutureMentorProperties properties;
    private final ConcurrentHashMap<String, ArrayDeque<Long>> hits = new ConcurrentHashMap<>();
    private final AtomicInteger operations = new AtomicInteger();

    public MentorRateLimiter(FutureMentorProperties properties) {
        this.properties = properties;
    }

    /**
     * Records one request for {@code key} or throws {@link RateLimitedException} when
     * the student has already consumed their per-minute budget.
     */
    public void acquire(String key) {
        int limit = Math.max(1, properties.getRateLimit().getRequestsPerMinute());
        long now = System.currentTimeMillis();
        ArrayDeque<Long> window = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > WINDOW_MILLIS) {
                window.pollFirst();
            }
            if (window.size() >= limit) {
                throw new RateLimitedException(
                        "You've asked " + limit + " questions in the last minute — please wait a moment "
                        + "before asking again.");
            }
            window.addLast(now);
        }
        maybeEvictIdle(now);
    }

    /** Drop buckets that have fully aged out so the map cannot grow unbounded. */
    private void maybeEvictIdle(long now) {
        if (operations.incrementAndGet() % 512 != 0) {
            return;
        }
        List<String> stale = new ArrayList<>();
        hits.forEach((k, window) -> {
            synchronized (window) {
                if (window.isEmpty() || now - window.peekLast() > WINDOW_MILLIS) {
                    stale.add(k);
                }
            }
        });
        stale.forEach(hits::remove);
    }
}
