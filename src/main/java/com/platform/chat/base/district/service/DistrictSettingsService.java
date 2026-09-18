package com.platform.chat.base.district.service;

import com.platform.chat.base.district.dto.DistrictSettingsDto;
import com.platform.chat.core.modules.poll.dto.PollSettingsDto;
import com.platform.chat.base.district.entity.District;
import com.platform.chat.base.district.entity.DistrictSettings;
import com.platform.chat.base.district.repository.DistrictRepository;
import com.platform.chat.base.district.repository.DistrictSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Сервис настроек района (пер-районные, редактируются админом через панель).
 * Строка создаётся при первом обращении/при инициализации района; дефолты лежат
 * в полях сущности {@link DistrictSettings}, отдельного конфига нет.
 */
@Service
@RequiredArgsConstructor
public class DistrictSettingsService {

    private final DistrictSettingsRepository settingsRepository;
    private final DistrictRepository districtRepository;

    /**
     * Проставить строку настроек для района, если её ещё нет.
     * Возвращает существующую или новую (со значениями по умолчанию).
     */
    @Transactional
    public DistrictSettings getOrCreate(Long districtId) {
        return settingsRepository.findByDistrictId(districtId)
                .orElseGet(() -> {
                    District district = districtRepository.findById(districtId)
                            .orElseThrow(() -> new RuntimeException("Район не найден: " + districtId));
                    DistrictSettings settings = new DistrictSettings();
                    settings.setDistrict(district);
                    return settingsRepository.save(settings);
                });
    }

    /** Эффективные настройки выборов губернатора района. */
    @Transactional
    public PollSettingsDto getElectionSettings(Long districtId) {
        DistrictSettings s = getOrCreate(districtId);
        return new PollSettingsDto(
                s.getElectionDurationHours(),
                s.getElectionAliveWindowDays(),
                s.getElectionQuorumPercent(),
                s.getElectionMaxCandidates()
        );
    }

    /** Эффективные настройки обжалования/переизбрания (запланировано). */
    @Transactional
    public PollSettingsDto getRecallSettings(Long districtId) {
        DistrictSettings s = getOrCreate(districtId);
        return new PollSettingsDto(
                s.getRecallDurationHours(),
                s.getRecallAliveWindowDays(),
                s.getRecallQuorumPercent(),
                null
        );
    }

    /** Текущие настройки района для админской панели. */
    @Transactional
    public DistrictSettingsDto getSettings(Long districtId) {
        DistrictSettings s = getOrCreate(districtId);
        return toDto(s);
    }

    /** Обновить настройки района из админской панели. */
    @Transactional
    public DistrictSettingsDto updateSettings(Long districtId, DistrictSettingsDto dto) {
        DistrictSettings s = getOrCreate(districtId);
        s.setElectionDurationHours(dto.electionDurationHours());
        s.setElectionAliveWindowDays(dto.electionAliveWindowDays());
        s.setElectionQuorumPercent(dto.electionQuorumPercent());
        s.setElectionMaxCandidates(dto.electionMaxCandidates());
        s.setRecallDurationHours(dto.recallDurationHours());
        s.setRecallAliveWindowDays(dto.recallAliveWindowDays());
        s.setRecallQuorumPercent(dto.recallQuorumPercent());
        return toDto(settingsRepository.save(s));
    }

    private DistrictSettingsDto toDto(DistrictSettings s) {
        return new DistrictSettingsDto(
                s.getElectionDurationHours(),
                s.getElectionAliveWindowDays(),
                s.getElectionQuorumPercent(),
                s.getElectionMaxCandidates(),
                s.getRecallDurationHours(),
                s.getRecallAliveWindowDays(),
                s.getRecallQuorumPercent()
        );
    }
}
