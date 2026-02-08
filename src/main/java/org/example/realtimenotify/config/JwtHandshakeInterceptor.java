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

  private final JwtService jwtService = new JwtService();

  private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

  @Override
  public boolean beforeHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    String token = null;

    List<String> auth = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
    if (auth != null && !auth.isEmpty()) {
      token = auth.get(0).replace("Bearer ", "");
    }

    log.debug("Attempting WS handshake from URI={} headersPresent={}", request.getURI(), auth != null && !auth.isEmpty());

    if (token == null) {
      URI uri = request.getURI();
      String query = uri.getQuery();
      if (query != null) {
        try {
          for (String kv : query.split("&")) {
            String[] parts = kv.split("=", 2);
            if (parts.length == 2) {
              String name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
              String value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
              if ("token".equals(name) || "access_token".equals(name)) {
                token = value;
                break;
              }
            }
          }
        } catch (Exception e) {
          // ignore parse errors
        }
      }
    }

    if (token == null || token.isEmpty()) {
      log.debug("Got empty token");
      return false;
    }

    String username = jwtService.validateAndGetUsername(token);
    if (username == null) {
      return false;
    }
    attributes.put("username", username);
    return true;
  }

  @Override
  public void afterHandshake(
      ServerHttpRequest request,
      ServerHttpResponse response,
      WebSocketHandler wsHandler,
      Exception exception) {}
}
