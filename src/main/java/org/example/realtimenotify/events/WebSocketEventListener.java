package org.example.realtimenotify.events;

import org.example.realtimenotify.service.PresenceService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

  private final PresenceService presenceService;

  public WebSocketEventListener(PresenceService presenceService) {
    this.presenceService = presenceService;
  }

  @EventListener
  public void handleSessionConnected(SessionConnectedEvent event) {
    StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
    if (sha.getUser() != null) {
      String user = sha.getUser().getName();
      presenceService.markOnline(user);
    }
  }

  @EventListener
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
    if (sha.getUser() != null) {
      String user = sha.getUser().getName();
      presenceService.markOffline(user);
    }
  }
}
