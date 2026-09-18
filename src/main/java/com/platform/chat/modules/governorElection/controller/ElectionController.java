package com.platform.chat.modules.governorElection.controller;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.chatUser.enums.PersonLevel;
import com.platform.chat.base.chatUser.service.ChatUserService;
import com.platform.chat.base.chatUser.service.PersonLevelService;
import com.platform.chat.core.modules.poll.dto.DistrictElectionView;
import com.platform.chat.core.modules.poll.entity.Poll;
import com.platform.chat.core.modules.poll.entity.PollCandidate;
import com.platform.chat.core.modules.poll.entity.PollVote;
import com.platform.chat.modules.governorElection.service.ElectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Слой 1. REST-контроллер выборов губернатора.
 * Принимает запросы от клиента (Angular), делегирует в ElectionFacade.
 */
@RestController
@RequestMapping("/api/elections")
@RequiredArgsConstructor
public class ElectionController {

    private final ElectionService electionService;
    private final ChatUserService chatUserService;
    private final PersonLevelService personLevelService;

    /**
     * Шаг 1: Инициировать выборы в районе.
     * Инициатор автоматически становится первым кандидатом.
     */
    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(
            @RequestParam Long districtId,
            Principal principal) {
        ChatUser user = chatUserService.getChatUserByUserName(principal.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Персонаж не найден"));
        }
        // Гейт 3-го уровня: только персонажи с LV3+ могут инициировать выборы
        if (!personLevelService.hasMinLevel(user, PersonLevel.LEVEL_3)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Требуется 3-й уровень персонажа"));
        }
        try {
            Poll poll = electionService.initiateElection(districtId, user.getId());
            return ResponseEntity.ok(poll);
        } catch (IllegalStateException | IllegalArgumentException e) {
            // Гейт-условие сценария №2: нет губернатора / нет параллельных выборов
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    /**
     * Шаг 2: Присоединиться к выборам как кандидат.
     */
    @PostMapping("/{pollId}/join")
    public ResponseEntity<?> join(
            @PathVariable Long pollId,
            Principal principal) {
        ChatUser user = chatUserService.getChatUserByUserName(principal.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Персонаж не найден"));
        }
        try {
            PollCandidate candidate = electionService.joinAsCandidate(pollId, user.getId());
            return ResponseEntity.ok(candidate);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Шаг 3: Проголосовать за кандидата.
     */
    @PostMapping("/{pollId}/vote")
    public ResponseEntity<?> vote(
            @PathVariable Long pollId,
            @RequestParam Long candidateId,
            Principal principal) {
        ChatUser user = chatUserService.getChatUserByUserName(principal.getName());
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Персонаж не найден"));
        }
        try {
            PollVote vote = electionService.castVote(pollId, user.getId(), candidateId);
            return ResponseEntity.ok(vote);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Шаг 4: Закрыть выборы и обработать результат (вручную).
     */
    @PostMapping("/{pollId}/close")
    public ResponseEntity<?> close(@PathVariable Long pollId) {
        try {
            Poll closed = electionService.closeElection(pollId);
            return ResponseEntity.ok(closed);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Получить статус голосования.
     */
    @GetMapping("/{pollId}")
    public ResponseEntity<Poll> getStatus(@PathVariable Long pollId) {
        try {
            Poll poll = electionService.getStatus(pollId);
            return ResponseEntity.ok(poll);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Получить список кандидатов.
     */
    @GetMapping("/{pollId}/candidates")
    public ResponseEntity<List<PollCandidate>> getCandidates(@PathVariable Long pollId) {
        return ResponseEntity.ok(electionService.getCandidates(pollId));
    }

    /**
     * Получить результаты голосования.
     */
    @GetMapping("/{pollId}/results")
    public ResponseEntity<Map<Long, Integer>> getResults(@PathVariable Long pollId) {
        return ResponseEntity.ok(electionService.getResults(pollId));
    }

    /**
     * Получить все выборы в районе.
     */
    @GetMapping("/district/{districtId}")
    public ResponseEntity<List<Poll>> getByDistrict(@PathVariable Long districtId) {
        return ResponseEntity.ok(electionService.getElectionsInDistrict(districtId));
    }

    /**
     * Состояние выборов района для мини-таблицы на Площади Ленина
     * (активное голосование либо результат последнего).
     */
    @GetMapping("/district/{districtId}/overview")
    public ResponseEntity<DistrictElectionView> getOverview(
            @PathVariable Long districtId,
            Principal principal) {
        ChatUser user = principal == null ? null
                : chatUserService.getChatUserByUserName(principal.getName());
        Long characterId = user == null ? null : user.getId();
        return ResponseEntity.ok(electionService.getDistrictOverview(districtId, characterId));
    }

    /** Единый ответ 400 для гейтов и недопустимых состояний. */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, String>> handleBadState(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
    }
}