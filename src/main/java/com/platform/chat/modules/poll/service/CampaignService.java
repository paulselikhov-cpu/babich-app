package com.platform.chat.modules.poll.service;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.modules.poll.entity.Campaign;
import com.platform.chat.modules.poll.entity.CampaignContribution;
import com.platform.chat.modules.poll.enums.CampaignType;
import com.platform.chat.modules.poll.enums.ScopeType;
import com.platform.chat.modules.poll.repository.CampaignContributionRepository;
import com.platform.chat.modules.poll.repository.CampaignRepository;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Слой 2 (Use Cases). Кампании / сбор подписей.
 * Управляет ивентом "Обжаловать губернатора и запустить переизбрание".
 */
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignContributionRepository contributionRepository;
    private final ChatUserRepository chatUserRepository;

    /**
     * Подсчёт живых участников района (были онлайн последние 2 суток).
     */
    public long countAliveResidents(Long districtId) {
        return chatUserRepository.countByDistrictIdAndLastSeenAfter(
                districtId, LocalDateTime.now().minusDays(2));
    }

    /**
     * Создать кампанию на отзыв мэра.
     * Для успеха нужно >70% подписей от живых участников района.
     */
    @Transactional
    public Campaign startRecallMayor(Long districtId, Long initiatorCharacterId) {
        // Проверяем, нет ли уже активной кампании
        Optional<Campaign> existing = campaignRepository.findByScopeTypeAndScopeIdAndCampaignTypeAndStatus(
                ScopeType.DISTRICT.name(), districtId, CampaignType.RECALL_MAYOR, "ACTIVE");
        if (existing.isPresent()) {
            throw new IllegalStateException("В районе уже идёт кампания по отзыву мэра");
        }

        long aliveCount = countAliveResidents(districtId);
        // 70% от живых, минимум 1
        int threshold = Math.max(1, (int) Math.ceil(aliveCount * 0.7));

        Campaign campaign = new Campaign();
        campaign.setCampaignType(CampaignType.RECALL_MAYOR);
        campaign.setScopeType(ScopeType.DISTRICT);
        campaign.setScopeId(districtId);
        campaign.setInitiatedByCharacterId(initiatorCharacterId);
        campaign.setGoalThreshold(threshold);
        campaign.setCurrentCount(1); // инициатор автоматически подписывается
        campaign.setStatus("ACTIVE");

        Campaign saved = campaignRepository.save(campaign);

        // Автоматическая подпись инициатора
        CampaignContribution contribution = new CampaignContribution();
        contribution.setCampaignId(saved.getId());
        contribution.setCharacterId(initiatorCharacterId);
        contributionRepository.save(contribution);

        return saved;
    }

    /**
     * Персонаж поддерживает кампанию (ставит подпись).
     */
    @Transactional
    public Campaign sign(Long campaignId, Long characterId) {
        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Кампания не найдена: " + campaignId));

        if (!"ACTIVE".equals(campaign.getStatus())) {
            throw new IllegalStateException("Кампания уже завершена");
        }

        if (contributionRepository.existsByCampaignIdAndCharacterId(campaignId, characterId)) {
            throw new IllegalStateException("Персонаж уже подписал кампанию");
        }

        // Проверяем, что персонаж жив
        ChatUser character = chatUserRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("Персонаж не найден: " + characterId));
        if (character.getLastSeen() == null ||
                character.getLastSeen().isBefore(LocalDateTime.now().minusDays(2))) {
            throw new IllegalStateException("Персонаж не является живым участником района");
        }

        CampaignContribution contribution = new CampaignContribution();
        contribution.setCampaignId(campaignId);
        contribution.setCharacterId(characterId);
        contributionRepository.save(contribution);

        campaign.setCurrentCount(campaign.getCurrentCount() + 1);

        // Если порог достигнут — кампания успешна
        if (campaign.getCurrentCount() >= campaign.getGoalThreshold()) {
            campaign.setStatus("SUCCESSFUL");
        }

        return campaignRepository.save(campaign);
    }

    /**
     * Проверить статус активной кампании отзыва мэра в районе.
     */
    public Optional<Campaign> getActiveRecallCampaign(Long districtId) {
        return campaignRepository.findByScopeTypeAndScopeIdAndCampaignTypeAndStatus(
                ScopeType.DISTRICT.name(), districtId, CampaignType.RECALL_MAYOR, "ACTIVE");
    }

    /**
     * Получить кампанию по ID.
     */
    public Campaign getById(Long campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Кампания не найдена: " + campaignId));
    }
}