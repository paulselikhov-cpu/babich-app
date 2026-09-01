package com.platform.civic.controller;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.service.chatUser.ChatUserService;
import com.platform.civic.entity.Application;
import com.platform.civic.enums.ApplicationType;
import com.platform.civic.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

/**
 * Гражданские заявки. В MVP реализована только покупка первой локации.
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ChatUserService chatUserService;

    @PostMapping("/buy-first-location")
    public ResponseEntity<Application> buyFirstLocation(Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        if (chatUser == null) {
            return ResponseEntity.badRequest().build();
        }
        Application application = applicationService.submit(
                chatUser.getUserId(), ApplicationType.BUY_FIRST_LOCATION, null);
        return ResponseEntity.ok(application);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Application>> getMyApplications(Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        if (chatUser == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(applicationService.getMyApplications(chatUser.getUserId()));
    }
}
