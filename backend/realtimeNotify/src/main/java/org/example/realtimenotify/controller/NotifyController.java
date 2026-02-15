package org.example.realtimenotify.controller;

import java.util.Map;
import org.example.realtimenotify.model.Notification;
import org.example.realtimenotify.service.NotificationService;
import org.example.realtimenotify.service.PresenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notify")
public class NotifyController {
  private final Logger log = LoggerFactory.getLogger(NotifyController.class);
  private final NotificationService notificationService;
  private final PresenceService presenceService;
  private final SimpMessagingTemplate template;

  public NotifyController(
      NotificationService notificationService,
      PresenceService presenceService,
      SimpMessagingTemplate template) {
    this.notificationService = notificationService;
    this.presenceService = presenceService;
    this.template = template;
  }

  @PostMapping
  public String notifyUser(@RequestBody Map<String, String> payload) {
    String userId = payload.get("userId");
    String message = payload.get("message");
    log.info("REST /notify -> user={}, message={}", userId, message);

    Notification n = notificationService.saveNotification(userId, message);

    log.info("About to convertAndSendToUser user={} payloadId={}", userId, n.getId());
    try {
      template.convertAndSendToUser(userId, "/queue/notifications", n);
      log.info("convertAndSendToUser SUCCESS for user={} id={}", userId, n.getId());
    } catch (Exception ex) {
      log.warn(
          "convertAndSendToUser FAILED for user={} id={} ex={}", userId, n.getId(), ex.toString());
    }

    return "notificationEnqueued:" + n.getId();
  }
}
