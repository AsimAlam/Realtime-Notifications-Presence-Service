package org.example.realtimenotify.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * RateLimiterService: if Redis present uses simple increment-with-expire; otherwise uses an
 * in-memory per-second counter as a fallback.
 *
 * <p>Note: in-memory fallback is NOT distributed and resets on app restart.
 */
@Service
public class RateLimiterService {

  private final StringRedisTemplate redis; // may be null
  private final int limitPerSecond = 10; // change as needed

  // In-memory fallback: map user -> entry
  private static class InMemoryBucket {
    AtomicInteger count = new AtomicInteger(0);
    volatile long lastSecond = 0L;
  }

  private final ConcurrentHashMap<String, InMemoryBucket> buckets = new ConcurrentHashMap<>();

  public RateLimiterService(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public boolean allow(String userId) {
    if (redis != null) {
      // Redis-based simple window: increment and set TTL if first
      String k = "rate:" + userId;
      Long v = redis.opsForValue().increment(k);
      if (v == 1) {
        redis.expire(k, java.time.Duration.ofSeconds(1));
      }
      return v <= limitPerSecond;
    } else {
      long nowSec = Instant.now().getEpochSecond();
      InMemoryBucket b = buckets.computeIfAbsent(userId, k -> new InMemoryBucket());
      synchronized (b) {
        if (b.lastSecond != nowSec) {
          b.lastSecond = nowSec;
          b.count.set(0);
        }
        int c = b.count.incrementAndGet();
        return c <= limitPerSecond;
      }
    }
  }
}
