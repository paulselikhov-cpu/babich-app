package com.platform.chat.modules.economy.controller;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.modules.economy.service.WorkService;
import com.platform.chat.service.chatUser.ChatUserService;
import com.platform.chat.modules.economy.dto.WorkResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.security.Principal;

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
}
