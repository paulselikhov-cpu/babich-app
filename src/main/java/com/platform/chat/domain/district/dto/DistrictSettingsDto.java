package com.platform.chat.domain.district.dto;

/**
 * DTO настроек района для админской панели (GET/PUT /api/districts/{id}/settings).
 * Покрывает обе группы: выборы губернатора и обжалование/переизбрание.
 */
public record DistrictSettingsDto(
        int electionDurationHours,
        int electionAliveWindowDays,
        int electionQuorumPercent,
        int electionMaxCandidates,
        int recallDurationHours,
        int recallAliveWindowDays,
        int recallQuorumPercent
) {
}
