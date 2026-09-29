package com.platform.chat.core.modules.notification.repository;

import com.platform.chat.core.modules.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientCharacterIdOrderByCreatedAtDesc(Long recipientCharacterId);

    long countByRecipientCharacterIdAndReadFalse(Long recipientCharacterId);
}