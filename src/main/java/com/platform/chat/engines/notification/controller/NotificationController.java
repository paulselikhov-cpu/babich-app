package com.platform.chat.engines.notification.controller;

import com.platform.chat.engines.notification.dto.NotificationResponse;
import com.platform.chat.engines.notification.entity.Notification;
import com.platform.chat.engines.notification.service.NotificationService;

import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.chatUser.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Нужен модалке уведомлений (колокольчик): история при заходе + отметки
 * прочитанным/подтверждение действий. Живые события приходят по WS
 * (/user/{username}/queue/notifications) — см. NotificationEventListener.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final ChatUserRepository chatUserRepository;

    /** Все уведомления текущего персонажа (свежие сверху). */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> list(Principal principal) {
        ChatUser character = resolveCharacter(principal);
        if (character == null) {
            return ResponseEntity.notFound().build();
        }
        List<NotificationResponse> result = notificationService.getNotifications(character.getId())
                .stream().map(NotificationResponse::from).toList();
        return ResponseEntity.ok(result);
    }

    /** Число непрочитанных уведомлений (для бейджа на колокольчике). */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> unreadCount(Principal principal) {
        ChatUser character = resolveCharacter(principal);
        if (character == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(notificationService.countUnread(character.getId()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markRead(@PathVariable Long id) {
        return respond(notificationService.markRead(id));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<NotificationResponse> accept(@PathVariable Long id) {
        return respond(notificationService.acceptAction(id));
    }

    @PostMapping("/{id}/decline")
    public ResponseEntity<NotificationResponse> decline(@PathVariable Long id) {
        return respond(notificationService.declineAction(id));
    }

    private ResponseEntity<NotificationResponse> respond(Notification n) {
        if (n == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(NotificationResponse.from(n));
    }

    private ChatUser resolveCharacter(Principal principal) {
        if (principal == null) {
            return null;
        }
        return chatUserRepository.findByUsername(principal.getName()).orElse(null);
    }
}
