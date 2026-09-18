package com.platform.chat.core.modules.poll.dto;

import java.util.List;

/**
 * Проекция состояния выборов губернатора в районе для фронтовой мини-таблицы
 * на Площади Ленина. Собирается ElectionFacade как ответ на
 * GET /api/elections/district/{districtId}/overview.
 *
 * @param active          есть ли релевантное голосование (активное либо последнее завершённое)
 * @param pollId          id голосования (null если выборов не было)
 * @param status          статус Poll (ACTIVE/PENDING/CLOSED)
 * @param endsAt          момент окончания в epoch-millis (null если закрыто/нет poll)
 * @param totalVoted      сколько голосов уже отдано
 * @param aliveCount      сколько живых участников в районе (были онлайн за 2 суток)
 * @param quorumThreshold сколько голосов требуется для кворума
 * @param quorumReached   достигнут ли кворум
 * @param myVoted         проголосовал ли текущий персонаж
 * @param myCandidate     является ли текущий персонаж кандидатом
 * @param winnerName      имя победителя (для CLOSED)
 * @param candidates      кандидаты/слоты 1–5 с голосами
 */
public record DistrictElectionView(
        boolean active,
        Long pollId,
        String status,
        Long endsAt,
        int totalVoted,
        int aliveCount,
        int quorumThreshold,
        boolean quorumReached,
        boolean myVoted,
        boolean myCandidate,
        String winnerName,
        List<CandidateView> candidates
) {

    /** Пустой ответ, когда выборов в районе ещё не было. */
    public static final DistrictElectionView EMPTY = new DistrictElectionView(
            false, null, null, null, 0, 0, 0, false, false, false, null, List.of());

    /** Кандидат/слот голосования: пустые слоты имеют characterId == null и name == null. */
    public record CandidateView(
            int slotNumber,
            Long candidateId,
            Long characterId,
            String name,
            long votes
    ) {}
}
