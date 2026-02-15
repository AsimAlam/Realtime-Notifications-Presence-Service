package org.example.realtimenotify.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class RateLimiterService {

  private final StringRedisTemplate redis;
  private final int ratePerSecond;
  private final int burstCapacity;
  private String luaScript;

  public RateLimiterService(StringRedisTemplate redis) {
    this.redis = redis;
    this.ratePerSecond = 10; // change as needed
    this.burstCapacity = 20;
  }

  @PostConstruct
  public void loadScript() {
    try (InputStream in = new ClassPathResource("scripts/tokenBucket.lua").getInputStream()) {
      luaScript = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean allow(String userId) {
    if (redis == null) return true;
    String key = "ratelimit:" + userId;
    Object result = redis.execute((RedisCallback<Object>) connection -> {
      byte[] script = luaScript.getBytes(StandardCharsets.UTF_8);
      byte[][] keys = new byte[0][];
      byte[][] args = new byte[][] {
              key.getBytes(StandardCharsets.UTF_8),
              String.valueOf(ratePerSecond).getBytes(StandardCharsets.UTF_8),
              String.valueOf(burstCapacity).getBytes(StandardCharsets.UTF_8)
      };
      return connection.eval(script, ReturnType.INTEGER, 0, args);
    });
    if (result instanceof Long) {
      return ((Long) result) == 1L;
    }
    return true;
  }
}


