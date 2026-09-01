package com.platform.chat.modules.poll.repository;

import com.platform.chat.modules.poll.entity.CampaignContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignContributionRepository extends JpaRepository<CampaignContribution, Long> {

    long countByCampaignId(Long campaignId);

    boolean existsByCampaignIdAndCharacterId(Long campaignId, Long characterId);
}