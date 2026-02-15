package org.example.realtimenotify.events;

import org.example.realtimenotify.service.NotificationService;
import org.example.realtimenotify.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Listens for STOMP connect/disconnect events and updates presence using sessionId-aware API.
 */
@Component
public class WebSocketEventListener {

  private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

  private final PresenceService presenceService;
  private final NotificationService notificationService;
  private final SimpMessagingTemplate template;

  public WebSocketEventListener(PresenceService presenceService,
                                NotificationService notificationService,
                                SimpMessagingTemplate template) {
    this.presenceService = presenceService;
    this.notificationService = notificationService;
    this.template = template;
  }

  @EventListener
  public void handleSessionConnected(SessionConnectedEvent event) {
    StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
    if (sha.getUser() != null) {
      String user = sha.getUser().getName();
      String sessionId = sha.getSessionId(); // <-- important
      log.debug("STOMP CONNECT: user={}, sessionId={}", user, sessionId);
      presenceService.markOnline(user, sessionId);

      // replay any undelivered messages for this user
      try {
        notificationService.replayPendingUndelivered(user, template);
      } catch (Exception e) {
        log.warn("Failed to replay pending notifications for user {}: {}", user, e.getMessage());
      }
    } else {
      log.debug("STOMP CONNECT with no Principal. headers={}", sha.getMessageHeaders());
    }
  }

  @EventListener
  public void handleSessionDisconnect(SessionDisconnectEvent event) {
    StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
    if (sha.getUser() != null) {
      String user = sha.getUser().getName();
      String sessionId = sha.getSessionId();
      log.debug("STOMP DISCONNECT: user={}, sessionId={}", user, sessionId);
      presenceService.markOffline(user, sessionId);
    } else {
      log.debug("STOMP DISCONNECT with no Principal, sessionId={}", sha.getSessionId());
    }
  }
}


