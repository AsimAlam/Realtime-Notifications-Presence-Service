package org.example.realtimenotify.config;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

public class UserHandshakeHandler extends DefaultHandshakeHandler {

  @Override
  protected Principal determineUser(
      ServerHttpRequest request,
      org.springframework.web.socket.WebSocketHandler wsHandler,
      Map<String, Object> attributes) {
    Object u = attributes.get("username");
    String name = (u != null) ? u.toString() : "anonymous";
    return new StompPrincipal(name);
  }

  private static class StompPrincipal implements Principal {
    private final String name;

    public StompPrincipal(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }
  }
}
