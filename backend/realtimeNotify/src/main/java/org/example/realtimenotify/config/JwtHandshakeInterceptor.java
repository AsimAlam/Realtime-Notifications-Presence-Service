package org.example.realtimenotify.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.example.realtimenotify.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

public class JwtHandshakeInterceptor implements HandshakeInterceptor {

  private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

  private final JwtService jwtService = new JwtService();

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {

    URI uri = request.getURI();
    String path = uri.getPath();
    log.debug("WS handshake request path={}, query={}", path, uri.getQuery());

    if (path != null && path.endsWith("/info")) {
      log.debug("Allowing SockJS info probe without auth for path={}", path);
      return true;
    }

    String token = null;
    List<String> auth = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
    if (auth != null && !auth.isEmpty()) {
      token = auth.get(0).replace("Bearer ", "").trim();
      log.debug("Found Authorization header for WS handshake");
    }

    if (token == null || token.isEmpty()) {
      String query = uri.getQuery();
      if (query != null) {
        try {
          for (String kv : query.split("&")) {
            String[] parts = kv.split("=", 2);
            if (parts.length == 2) {
              String name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
              String value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
              if ("token".equalsIgnoreCase(name) || "access_token".equalsIgnoreCase(name)) {
                token = value;
                log.debug("Found token in query param");
                break;
              }
            }
          }
        } catch (Exception ex) {
          log.warn("Failed to parse query params for WS handshake: {}", ex.getMessage());
        }
      }
    }

    if (token == null || token.isEmpty()) {
      log.warn(
          "WebSocket handshake rejected because no token found. Request path={}, query={}",
          path,
          uri.getQuery());
      return false;
    }

    String username = jwtService.validateAndGetUsername(token);
    if (username == null) {
      log.warn("WebSocket handshake rejected: token validation failed");
      return false;
    }

    attributes.put("username", username);
    log.debug("WebSocket handshake allowed for user={}", username);
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {
    // no-op
  }
}
