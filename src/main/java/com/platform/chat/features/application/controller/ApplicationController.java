package com.platform.chat.features.application.controller;

import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.chatUser.service.ChatUserService;

import com.platform.chat.engines.application.dto.ApplicationSummary;
import com.platform.chat.engines.application.service.ApplicationQueryService;
import com.platform.chat.engines.application.service.ApplicationService;

import com.platform.chat.features.application.dto.ApplicationView;
import com.platform.chat.features.application.dto.ApplicationViewAssembler;
import com.platform.chat.features.application.service.ApplicationSubmissionService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * REST мэрии по гражданским заявкам: подача, модерация, списки. Контроллер только
 * транслирует HTTP в команды/запросы: доменные правила живут в сервисах
 * ({@code ApplicationSubmissionService}, ядро процесса), а наружу отдаются DTO,
 * не JPA-entity.
 *
 * <p>Список всех заявок (/all) — для модерации «Гражданские заявки»: пока без
 * гейта роли (отдаётся любому авторизованному; реальная модерация — далее).</p>
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private static final String DEFAULT_REJECT_REASON = "Отклонено по решению сотрудника мэрии";

    private final ApplicationSubmissionService submissionService;
    private final ApplicationService applicationService;
    private final ApplicationQueryService queryService;
    private final ApplicationViewAssembler viewAssembler;
    private final ChatUserService chatUserService;

    @PostMapping("/register-passport")
    public ResponseEntity<ApplicationSummary> registerPassport(Principal principal) {
        ChatUser character = requireCharacter(principal);
        return ResponseEntity.ok(submissionService.submitPassport(character));
    }

    @PostMapping("/register-work-licence")
    public ResponseEntity<ApplicationSummary> registerWorkLicence(Principal principal) {
        ChatUser character = requireCharacter(principal);
        return ResponseEntity.ok(submissionService.submitWorkLicence(character));
    }

    /** TODO ДЛЯ СОТРУДНИКА Мэрии, а не для системной роли. Одобрить заявку (только для сотрудника мэрии — ADMIN/MODERATOR). */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ApplicationSummary> approveApplication(
            @PathVariable Long id,
            Principal principal) {
        requireStaff(principal);
        return ResponseEntity.ok(ApplicationSummary.from(applicationService.approveApplication(id)));
    }

    /** Отклонить заявку (только для сотрудника мэрии — ADMIN/MODERATOR). */
    @PostMapping("/{id}/reject")
    public ResponseEntity<ApplicationSummary> rejectApplication(
            @PathVariable Long id,
            @RequestParam(defaultValue = DEFAULT_REJECT_REASON) String reason,
            Principal principal) {
        requireStaff(principal);
        return ResponseEntity.ok(ApplicationSummary.from(applicationService.rejectApplication(id, reason)));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ApplicationSummary>> getMyApplications(Principal principal) {
        ChatUser character = requireCharacter(principal);
        return ResponseEntity.ok(queryService.getMyApplications(character.getUserId()));
    }

    /**
     * Все гражданские заявки с именами заявителей (свежие сверху) —
     * для модератора/сотрудника мэрии в «Гражданских заявках».
     */
    @GetMapping("/all")
    public ResponseEntity<List<ApplicationView>> getAllApplications() {
        return ResponseEntity.ok(viewAssembler.assemble(queryService.getAllApplications()));
    }

    /**
     * Персонаж текущего запроса.
     */
    private ChatUser requireCharacter(Principal principal) {
        ChatUser character = chatUserService.getChatUserByUserName(principal.getName());
        if (character == null) {
            throw new RuntimeException("Персонаж не найден для пользователя: " + principal.getName());
        }
        return character;
    }

    /**
     * Модерация доступна только сотруднику мэрии (ADMIN/MODERATOR).
     * AccessDeniedException → 403 (см. GlobalExceptionHandler), фронт по 403
     * показывает «нет доступа».
     */
    private void requireStaff(Principal principal) {
        if (!chatUserService.isStaff(principal.getName())) {
            throw new AccessDeniedException("Нет прав сотрудника мэрии");
        }
    }
}
