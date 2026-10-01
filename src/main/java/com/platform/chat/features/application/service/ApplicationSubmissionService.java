package com.platform.chat.features.application.service;

import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.engines.application.dto.ApplicationSummary;
import com.platform.chat.engines.application.entity.Application;
import com.platform.chat.engines.application.enums.ApplicationMessageKey;
import com.platform.chat.engines.application.enums.ApplicationType;
import com.platform.chat.engines.application.exception.ApplicationSubmissionRejectedException;
import com.platform.chat.engines.application.i18n.ApplicationMessages;
import com.platform.chat.engines.application.service.ApplicationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Подача заявок: связывает персонажа ({@code chat.domain.chatUser}) с ядром процесса
 * заявки. Здесь живут правила, которым нужны оба модуля сразу — «у персонажа уже
 * есть такой документ» и «нельзя подать вторую незакрытую заявку того же типа».
 *
 * <p>Правила не могут жить в контроллере (это домен, а не HTTP) и не могут жить
 * в ядре процесса: ядро не знает про {@code ChatUser} — иначе вернулся бы
 * направленный цикл между ядром и исполнителями.</p>
 */
@Service
@RequiredArgsConstructor
public class ApplicationSubmissionService {

    private final ApplicationService applicationService;
    private final ApplicationMessages messages;

    /** Подать заявку на паспорт от имени персонажа. */
    public ApplicationSummary submitPassport(ChatUser character) {
        if (character.getPassportId() != null) {
            throw rejected(ApplicationMessageKey.PASSPORT_ALREADY_EXISTS);
        }
        return submit(character, ApplicationType.REGISTER_PASSPORT);
    }

    /** Подать заявку на рабочую лицензию от имени персонажа. */
    public ApplicationSummary submitWorkLicence(ChatUser character) {
        if (character.getWorkLicenceId() != null) {
            throw rejected(ApplicationMessageKey.WORK_LICENCE_ALREADY_EXISTS);
        }
        return submit(character, ApplicationType.REGISTER_WORK_LICENCE);
    }

    /**
     * Общая часть подачи: одна незакрытая заявка на тип, затем создание PENDING
     * (обработка — при одобрении сотрудником мэрии).
     */
    private ApplicationSummary submit(ChatUser character, ApplicationType type) {
        if (applicationService.existsPendingApplication(character.getId(), type)) {
            throw rejected(ApplicationMessageKey.APPLICATION_ALREADY_PENDING);
        }
        Application application = applicationService.createPendingApplication(
                character.getUserId(), character.getId(), type, null);
        return ApplicationSummary.from(application);
    }

    /** Отказ в подаче с текстом для фронта («Не удалось подать заявку: …»). */
    private ApplicationSubmissionRejectedException rejected(ApplicationMessageKey reason) {
        return new ApplicationSubmissionRejectedException(
                messages.resolve(ApplicationMessageKey.CANCELED) + ": " + messages.resolve(reason));
    }
}
