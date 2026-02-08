package org.example.realtimenotify.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.example.realtimenotify.model.Notification;
import org.example.realtimenotify.repo.NotificationRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/** NotificationService: uses Redis seq if available, otherwise falls back to in-memory seq map. */
@Service
public class NotificationService {

  private final NotificationRepository repo;
  // may be null if Redis auto-config not present
  private final StringRedisTemplate redis;

  // fallback in-memory per-user seq counters
  private final ConcurrentHashMap<String, AtomicLong> seqMap = new ConcurrentHashMap<>();

  public NotificationService(NotificationRepository repo, StringRedisTemplate redis) {
    this.repo = repo;
    this.redis = redis;
  }

  public Notification saveNotification(String toUserId, String payload) {
    Long seq = 0L;
    if (toUserId != null) {
      if (redis != null) {
        String key = "seq:" + toUserId;
        seq = redis.opsForValue().increment(key);
      } else {
        seq = seqMap.computeIfAbsent(toUserId, k -> new AtomicLong(0)).incrementAndGet();
      }
    }
    Notification n = new Notification(toUserId, payload, seq, Instant.now());
    return repo.save(n);
  }

  public void replayMissed(String userId, long lastSeenSeq, SimpMessagingTemplate template) {
    List<Notification> missed =
        repo.findByToUserIdAndSeqGreaterThanOrderBySeqAsc(userId, lastSeenSeq);
    for (Notification n : missed) {
      template.convertAndSendToUser(userId, "/queue/messages", n);
    }
  }
}
