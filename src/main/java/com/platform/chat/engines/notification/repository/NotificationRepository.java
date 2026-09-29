package com.platform.chat.engines.notification.repository;

import com.platform.chat.engines.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientCharacterIdOrderByCreatedAtDesc(Long recipientCharacterId);

    long countByRecipientCharacterIdAndReadFalse(Long recipientCharacterId);
}