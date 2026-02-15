package org.example.realtimenotify.controller;

import java.security.Principal;
import org.example.realtimenotify.model.Notification;
import org.example.realtimenotify.service.NotificationService;
import org.example.realtimenotify.service.RateLimiterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class MessagingController {

  private final SimpMessagingTemplate template;
  private final NotificationService notificationService;
  private final RateLimiterService rateLimiterService;

  private final Logger log = LoggerFactory.getLogger(MessagingController.class);

  public MessagingController(
      SimpMessagingTemplate template,
      NotificationService notificationService,
      RateLimiterService rateLimiterService) {
    this.template = template;
    this.notificationService = notificationService;
    this.rateLimiterService = rateLimiterService;
  }

  @MessageMapping("/send")
  public void handleMessage(@Payload ClientMessage msg, Principal principal) {
    String from = (principal != null) ? principal.getName() : "anonymous";
    log.info(
        "STOMP /send received - from={}, to={}, content={}",
        from,
        msg.getToUserId(),
        msg.getContent());
    Notification n = notificationService.saveNotification(msg.getToUserId(), msg.getContent());
    log.info("Sending via convertAndSendToUser to={} id={}", msg.getToUserId(), n.getId());
    template.convertAndSendToUser(msg.getToUserId(), "/queue/notifications", n);
  }

  @MessageMapping("/broadcast")
  public void broadcast(@Payload BroadcastMessage message) {
    Notification n = notificationService.saveNotification(null, message.getContent());
    template.convertAndSend("/topic/announcements", n);
  }

  @MessageMapping("/recover")
  public void recover(RecoverRequest req, Principal principal) {
    if (principal == null) return;
    String user = principal.getName();
    notificationService.replayMissed(user, req.getLastSeenSeq(), template);
  }

  @MessageMapping("/ack")
  public void handleAck(@Payload AckMessage ack, Principal principal) {
    if (ack.getNotificationId() != null) {
      notificationService.markDelivered(ack.getNotificationId());
    } else if (ack.getToUserId() != null && ack.getSeq() != null) {
    }
  }

  public static class AckMessage {
    private Long notificationId;
    private Long seq;
    private String toUserId;

    public Long getNotificationId() {
      return notificationId;
    }

    public void setNotificationId(Long notificationId) {
      this.notificationId = notificationId;
    }

    public Long getSeq() {
      return seq;
    }

    public void setSeq(Long seq) {
      this.seq = seq;
    }

    public String getToUserId() {
      return toUserId;
    }

    public void setToUserId(String s) {
      this.toUserId = s;
    }
  }

  public static class ClientMessage {
    private String toUserId;
    private String content;

    public String getToUserId() {
      return toUserId;
    }

    public void setToUserId(String toUserId) {
      this.toUserId = toUserId;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }
  }

  public static class BroadcastMessage {
    private String content;

    public String getContent() {
      return content;
    }

    public void setContent(String c) {
      this.content = c;
    }
  }

  public static class RecoverRequest {
    private long lastSeenSeq;

    public long getLastSeenSeq() {
      return lastSeenSeq;
    }

    public void setLastSeenSeq(long s) {
      this.lastSeenSeq = s;
    }
  }
}
