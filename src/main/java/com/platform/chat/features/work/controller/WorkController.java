package com.platform.chat.modules.work.controller;
import com.platform.chat.modules.work.service.WorkService;
import com.platform.chat.modules.work.dto.WorkResultResponse;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.chatUser.service.ChatUserService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;
import java.util.Map;

/**
 * Тап-фарм работы для персонажа.
 * В MVP доступна только одна профессия — дворник.
 */
@RestController
@RequestMapping("/api/work")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;
    private final ChatUserService chatUserService;

    @PostMapping("/dvornik")
    public ResponseEntity<WorkResultResponse> workAsDvornik(Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        if (chatUser == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(workService.workAsDvornik(chatUser.getId()));
    }

    /**
     * Доменные ошибки работы (нет профессии, кулдаун, нехватка энергии) —
     * как 400 с человекочитаемым сообщением для UI.
     */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleDomainErrors(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
