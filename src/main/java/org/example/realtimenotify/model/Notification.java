package org.example.realtimenotify.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String toUserId; // null for broadcast

  @Column(length = 2000)
  private String payload;

  private Long seq;

  private Instant createdAt;

  public Notification() {}

  public Notification(String toUserId, String payload, Long seq, Instant createdAt) {
    this.toUserId = toUserId;
    this.payload = payload;
    this.seq = seq;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getToUserId() {
    return toUserId;
  }

  public void setToUserId(String toUserId) {
    this.toUserId = toUserId;
  }

  public String getPayload() {
    return payload;
  }

  public void setPayload(String payload) {
    this.payload = payload;
  }

  public Long getSeq() {
    return seq;
  }

  public void setSeq(Long seq) {
    this.seq = seq;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
