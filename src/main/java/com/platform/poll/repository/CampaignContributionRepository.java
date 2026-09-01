package com.platform.poll.repository;

import com.platform.poll.entity.CampaignContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignContributionRepository extends JpaRepository<CampaignContribution, Long> {

    long countByCampaignId(Long campaignId);

    boolean existsByCampaignIdAndCharacterId(Long campaignId, Long characterId);
}