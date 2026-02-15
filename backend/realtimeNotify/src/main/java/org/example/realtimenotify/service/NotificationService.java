package org.example.realtimenotify.service;

import org.example.realtimenotify.model.Notification;
import org.example.realtimenotify.repo.NotificationRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NotificationService {

  private final NotificationRepository repo;
  private final StringRedisTemplate redis;
  // seq fallback if redis not present
  private final ConcurrentHashMap<String, AtomicLong> seqMap = new ConcurrentHashMap<>();

  public NotificationService(NotificationRepository repo, StringRedisTemplate redis) {
    this.repo = repo;
    this.redis = redis;
  }

  @Transactional
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
    Notification saved = repo.save(n);
    // push id to pending list for quick lookup (Redis list)
    if (redis != null && toUserId != null) {
      redis.opsForList().rightPush("pending:" + toUserId, saved.getId().toString());
    }
    return saved;
  }

  // deliver to user if online, else keep for replay (controller/service should call saveNotification first)
  public void deliverIfOnline(String toUserId, Notification n, SimpMessagingTemplate template, PresenceService presenceService) {
    boolean online = presenceService.isOnline(toUserId);
    if (online) {
      // send and optionally mark temporarily; final mark happens via ACK
      template.convertAndSendToUser(toUserId, "/queue/notifications", n);
    } else {
      // keep in DB & Redis pending list (already done by saveNotification)
    }
  }

  public void replayMissed(String userId, long lastSeenSeq, SimpMessagingTemplate template) {
    // Primary: deliver DB rows with seq > lastSeenSeq
    List<Notification> missed = repo.findByToUserIdAndSeqGreaterThanOrderBySeqAsc(userId, lastSeenSeq);
    for (Notification n : missed) {
      if (!n.isDelivered()) {
        template.convertAndSendToUser(userId, "/queue/notifications", n);
      }
    }
  }

  // deliver pending from DB (undelivered)
  public void replayPendingUndelivered(String userId, SimpMessagingTemplate template) {
    List<Notification> pending = repo.findByToUserIdAndDeliveredFalseOrderBySeqAsc(userId);
    for (Notification n : pending) {
      template.convertAndSendToUser(userId, "/queue/notifications", n);
    }
  }

  @Transactional
  public void markDelivered(Long notificationId) {
    repo.findById(notificationId).ifPresent(n -> {
      n.setDelivered(true);
      repo.save(n);
      // remove from pending list in redis if present
      if (redis != null && n.getToUserId() != null) {
        redis.opsForList().remove("pending:" + n.getToUserId(), 0, notificationId.toString());
      }
    });
  }

  // optional: get pending IDs from redis quickly
  public List<String> getRedisPendingIds(String userId) {
    if (redis == null) return List.of();
    return redis.opsForList().range("pending:" + userId, 0, -1);
  }
}

