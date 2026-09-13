package com.platform.chat.modules.application.controller;

import com.platform.auth.entity.User;
import com.platform.auth.repository.UserRepository;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.modules.application.enums.ApplicationMessageKey;
import com.platform.chat.modules.application.utils.ApplicationMessages;
import com.platform.chat.service.chatUser.ChatUserService;
import com.platform.chat.modules.application.dto.ApplicationView;
import com.platform.chat.modules.application.entity.Application;
import com.platform.chat.modules.application.enums.ApplicationType;
import com.platform.chat.modules.application.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

/**
 * Гражданские заявки. В MVP реализованы покупка первой локации и паспорт.
 * Список всех заявок (/all) — для модерации «Гражданские заявки» (пока без
 * гейта роли: отдаётся любому авторизованному; реальная модерация — далее).
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ChatUserService chatUserService;
    private final UserRepository userRepository;
    private final ApplicationMessages messages;

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

    @PostMapping("/register-passport")
    public ResponseEntity<Application> registerPassport(Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        if (chatUser == null) {
            return ResponseEntity.badRequest().build();
        }

        if (chatUser.getPassportId() != null) {
            throw new RuntimeException(
                    messages.resolve(ApplicationMessageKey.PASSPORT_ALREADY_EXISTS)
            );
        }
        // Создаём заявку в PENDING — обработка будет при одобрении сотрудником мэрии
        Application application = applicationService.createPendingApplication(
                chatUser.getUserId(), ApplicationType.REGISTER_PASSPORT, null);
        return ResponseEntity.ok(application);
    }

    @PostMapping("/register-work-licence")
    public ResponseEntity<Application> registerWorkLicence(Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        if (chatUser == null) {
            return ResponseEntity.badRequest().build();
        }

        if (chatUser.getPassportId() != null) {
            throw new RuntimeException(
                    messages.resolve(ApplicationMessageKey.WORK_LICENCE_ALREADY_EXISTS)
            );
        }
        // Создаём заявку в PENDING — обработка будет при одобрении сотрудником мэрии
        Application application = applicationService.createPendingApplication(
                chatUser.getUserId(), ApplicationType.REGISTER_WORK_LICENCE, null);
        return ResponseEntity.ok(application);
    }

    /** Список заявок на получение паспорта (для модерации сотрудником мэрии). */
    @GetMapping("/register-passport")
    public ResponseEntity<List<ApplicationView>> getRegisterPassportApplications() {
        return ResponseEntity.ok(
                applicationService.getRegisterPassportApplications().stream()
                        .map(this::toView)
                        .toList());
    }

    /** Одобрить заявку (только для сотрудника мэрии — ADMIN/MODERATOR). */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Application> approveApplication(
            @PathVariable Long id,
            Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        if (chatUser == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!chatUserService.isStaff(principal.getName())) {
            return ResponseEntity.status(403).build();
        }
        Application application = applicationService.approveApplication(id);
        return ResponseEntity.ok(application);
    }

    /** Отклонить заявку (только для сотрудника мэрии — ADMIN/MODERATOR). */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Application> rejectApplication(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Отклонено по решению сотрудника мэрии") String reason,
            Principal principal) {
        ChatUser chatUser = chatUserService.getChatUserByUserName(principal.getName());
        if (chatUser == null) {
            return ResponseEntity.badRequest().build();
        }
        if (!chatUserService.isStaff(principal.getName())) {
            return ResponseEntity.status(403).build();
        }
        Application application = applicationService.rejectApplication(id, reason);
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

    /**
     * Все гражданские заявки с именами заявителей (свежие сверху) —
     * для модератора/сотрудника мэрии в «Гражданских заявках».
     */
    @GetMapping("/all")
    public ResponseEntity<List<ApplicationView>> getAllApplications() {
        return ResponseEntity.ok(
                applicationService.getAllApplications().stream()
                        .map(this::toView)
                        .toList());
    }

    private ApplicationView toView(Application app) {
        String username = userRepository.findById(app.getUserId())
                .map(User::getUsername)
                .orElse(null);
        return new ApplicationView(
                app.getId(),
                app.getType(),
                app.getStatus(),
                app.getResultMessage(),
                app.getCreatedAt(),
                app.getProcessedAt(),
                app.getUserId(),
                username);
    }
}
