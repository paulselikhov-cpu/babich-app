package com.platform.poll.repository;

import com.platform.poll.entity.Campaign;
import com.platform.poll.enums.CampaignType;
import com.platform.poll.enums.ScopeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findByScopeTypeAndScopeId(String scopeType, Long scopeId);

    Optional<Campaign> findByScopeTypeAndScopeIdAndCampaignTypeAndStatus(
            String scopeType, Long scopeId, CampaignType campaignType, String status);

    List<Campaign> findByStatus(String status);
}