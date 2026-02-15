package org.example.realtimenotify.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;

public class JwtService {

  private final Key key;
  private final long expirationMs = 24 * 60 * 60 * 1000L;

  public JwtService() {
    this.key = Keys.hmacShaKeyFor("secret-key-for-dev-only-please-changesecret-key".getBytes());
  }

  public String generateToken(String username) {
    Date now = new Date();
    Date exp = new Date(now.getTime() + expirationMs);
    return Jwts.builder()
        .setSubject(username)
        .setIssuedAt(now)
        .setExpiration(exp)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public String validateAndGetUsername(String token) {
    try {
      return Jwts.parserBuilder()
          .setSigningKey(key)
          .build()
          .parseClaimsJws(token)
          .getBody()
          .getSubject();
    } catch (Exception e) {
      return null;
    }
  }
}
