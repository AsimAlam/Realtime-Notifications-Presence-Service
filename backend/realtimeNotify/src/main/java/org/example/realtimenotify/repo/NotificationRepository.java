package org.example.realtimenotify.repo;

import java.util.List;
import org.example.realtimenotify.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
  List<Notification> findByToUserIdAndSeqGreaterThanOrderBySeqAsc(String toUserId, Long seq);

  List<Notification> findByToUserIdAndDeliveredFalseOrderBySeqAsc(String toUserId);
}
