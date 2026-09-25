package com.platform.chat.modules.application.service;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.core.modules.application.dto.ApplicationSummary;
import com.platform.chat.core.modules.application.entity.Application;
import com.platform.chat.core.modules.application.enums.ApplicationMessageKey;
import com.platform.chat.core.modules.application.enums.ApplicationType;
import com.platform.chat.core.modules.application.exception.ApplicationSubmissionRejectedException;
import com.platform.chat.core.modules.application.i18n.ApplicationMessages;
import com.platform.chat.core.modules.application.service.ApplicationService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Подача заявок: связывает персонажа ({@code chat.base.chatUser}) с ядром процесса
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

    /** Отказ в подаче: тот же текст, что показывался фронту раньше («Не удалось подать заявку: …»). */
    private ApplicationSubmissionRejectedException rejected(ApplicationMessageKey reason) {
        return new ApplicationSubmissionRejectedException(
                messages.resolve(ApplicationMessageKey.CANCELED) + ": " + messages.resolve(reason));
    }
}
