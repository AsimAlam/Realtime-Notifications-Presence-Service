package org.example.realtimenotify.service;

import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/** used Redis set if available else in-memory used */
@Service
public class PresenceService {
  private final StringRedisTemplate redis;
  private static final String GLOBAL_SET_KEY = "presence:users";

  private final ConcurrentHashMap<String, java.util.Set<String>> sessions =
      new ConcurrentHashMap<>();

  public PresenceService(@Autowired(required = false) StringRedisTemplate redis) {
    this.redis = redis;
  }

  public void markOnline(String userId, String sessionId) {
    if (redis != null) {
      String sessionsKey = "presence:user:" + userId + ":sessions";
      redis.opsForSet().add(sessionsKey, sessionId);
      redis.opsForSet().add(GLOBAL_SET_KEY, userId);
      redis.expire(sessionsKey, java.time.Duration.ofMinutes(10));
    } else {
      sessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }
  }

  public void markOffline(String userId, String sessionId) {
    if (redis != null) {
      String sessionsKey = "presence:user:" + userId + ":sessions";
      redis.opsForSet().remove(sessionsKey, sessionId);
      Long remaining = redis.opsForSet().size(sessionsKey);
      if (remaining == null || remaining == 0L) {
        redis.opsForSet().remove(GLOBAL_SET_KEY, userId);
      }
    } else {
      sessions.computeIfPresent(
          userId,
          (k, set) -> {
            set.remove(sessionId);
            return set.isEmpty() ? null : set;
          });
    }
  }

  public boolean isOnline(String userId) {
    if (redis != null) {
      Boolean member = redis.opsForSet().isMember(GLOBAL_SET_KEY, userId);
      return Boolean.TRUE.equals(member);
    } else {
      java.util.Set<String> set = sessions.get(userId);
      return set != null && !set.isEmpty();
    }
  }

  public java.util.Set<String> getOnlineUsers() {
    if (redis != null) {
      java.util.Set<String> members = redis.opsForSet().members(GLOBAL_SET_KEY);
      return members == null ? java.util.Collections.emptySet() : members;
    } else {
      return java.util.Collections.unmodifiableSet(sessions.keySet());
    }
  }
}
