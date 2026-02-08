package org.example.realtimenotify.service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/** PresenceService: uses Redis set if available, otherwise in-memory concurrent set. */
@Service
public class PresenceService {
  private final RedisTemplate<String, String> redis; // may be null
  private final String key = "presence:users";

  // In-memory fallback set
  private final ConcurrentHashMap<String, Boolean> memoryPresence = new ConcurrentHashMap<>();

  public PresenceService(RedisTemplate<String, String> redis) {
    this.redis = redis;
  }

  public void markOnline(String userId) {
    if (redis != null) {
      redis.opsForSet().add(key, userId);
    } else {
      memoryPresence.put(userId, Boolean.TRUE);
    }
  }

  public void markOffline(String userId) {
    if (redis != null) {
      redis.opsForSet().remove(key, userId);
    } else {
      memoryPresence.remove(userId);
    }
  }

  public Set<String> getOnlineUsers() {
    if (redis != null) {
      return redis.opsForSet().members(key);
    } else {
      return Collections.unmodifiableSet(memoryPresence.keySet());
    }
  }

  public boolean isOnline(String userId) {
    if (redis != null) {
      Boolean member = redis.opsForSet().isMember(key, userId);
      return Boolean.TRUE.equals(member);
    } else {
      return memoryPresence.containsKey(userId);
    }
  }
}
