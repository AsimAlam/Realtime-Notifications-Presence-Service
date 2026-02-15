package org.example.realtimenotify.repo;

import org.example.realtimenotify.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByToUserIdAndSeqGreaterThanOrderBySeqAsc(String toUserId, Long seq);
  List<Notification> findByToUserIdAndDeliveredFalseOrderBySeqAsc(String toUserId);
}

