package com.platform.chat.modules.poll.controller;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.enums.PersonLevel;
import com.platform.chat.service.chatUser.ChatUserService;
import com.platform.chat.service.level.PersonLevelService;
import com.platform.chat.modules.poll.entity.Poll;
import com.platform.chat.modules.poll.entity.PollCandidate;
import com.platform.chat.modules.poll.entity.PollVote;
import com.platform.chat.modules.poll.service.ElectionFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Слой 1 (Интерфейсы). REST-контроллер выборов губернатора.
 * Принимает запросы от клиента (Angular), делегирует в ElectionFacade.
 */
@RestController
@RequestMapping("/api/elections")
@RequiredArgsConstructor
public class ElectionController {

    private final ElectionFacade electionFacade;
    private final ChatUserService chatUserService;
    private final PersonLevelService personLevelService;

    /**
     * Шаг 1: Инициировать выборы в районе.
     * Инициатор автоматически становится первым кандидатом.
     */
    @PostMapping("/initiate")
    public ResponseEntity<Poll> initiate(
            @RequestParam Long districtId,
            Principal principal) {
        ChatUser user = chatUserService.getChatUserByUserName(principal.getName());
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        // Гейт 3-го уровня: только персонажи с LV3+ могут инициировать выборы
        if (!personLevelService.hasMinLevel(user, PersonLevel.LEVEL_3)) {
            return ResponseEntity.badRequest().body(null);
        }
        Poll poll = electionFacade.initiateElection(districtId, user.getUserId());
        return ResponseEntity.ok(poll);
    }
    /**
     * Шаг 2: Присоединиться к выборам как кандидат.
     */
    @PostMapping("/{pollId}/join")
    public ResponseEntity<PollCandidate> join(
            @PathVariable Long pollId,
            Principal principal) {
        ChatUser user = chatUserService.getChatUserByUserName(principal.getName());
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            PollCandidate candidate = electionFacade.joinAsCandidate(pollId, user.getUserId());
            return ResponseEntity.ok(candidate);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Шаг 3: Проголосовать за кандидата.
     */
    @PostMapping("/{pollId}/vote")
    public ResponseEntity<PollVote> vote(
            @PathVariable Long pollId,
            @RequestParam Long candidateId,
            Principal principal) {
        ChatUser user = chatUserService.getChatUserByUserName(principal.getName());
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        try {
            PollVote vote = electionFacade.castVote(pollId, user.getUserId(), candidateId);
            return ResponseEntity.ok(vote);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * Шаг 4: Закрыть выборы и обработать результат (вручную).
     */
    @PostMapping("/{pollId}/close")
    public ResponseEntity<Poll> close(@PathVariable Long pollId) {
        try {
            Poll closed = electionFacade.closeElection(pollId);
            return ResponseEntity.ok(closed);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Получить статус голосования.
     */
    @GetMapping("/{pollId}")
    public ResponseEntity<Poll> getStatus(@PathVariable Long pollId) {
        try {
            Poll poll = electionFacade.getStatus(pollId);
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
        return ResponseEntity.ok(electionFacade.getCandidates(pollId));
    }

    /**
     * Получить результаты голосования.
     */
    @GetMapping("/{pollId}/results")
    public ResponseEntity<Map<Long, Integer>> getResults(@PathVariable Long pollId) {
        return ResponseEntity.ok(electionFacade.getResults(pollId));
    }

    /**
     * Получить все выборы в районе.
     */
    @GetMapping("/district/{districtId}")
    public ResponseEntity<List<Poll>> getByDistrict(@PathVariable Long districtId) {
        return ResponseEntity.ok(electionFacade.getElectionsInDistrict(districtId));
    }
}