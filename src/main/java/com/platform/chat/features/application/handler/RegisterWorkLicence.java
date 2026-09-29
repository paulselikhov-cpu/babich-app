package com.platform.chat.features.application.handler;

import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.chatUser.service.ChatUserService;

import com.platform.chat.engines.application.entity.Application;
import com.platform.chat.engines.application.enums.ApplicationMessageKey;
import com.platform.chat.engines.application.enums.ApplicationType;
import com.platform.chat.engines.application.model.ApplicationDecision;
import com.platform.chat.engines.application.model.ApplicationHandler;
import com.platform.chat.engines.application.model.DocumentNumberGenerator;
import com.platform.chat.engines.domainEvent.events.CharacterUpdatedEvent;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Стратегия заявки «регистрация рабочей лицензии»: выдаёт персонажу разрешение
 * на работу. Эффект применяется через {@link ChatUserService} (API модуля
 * персонажа, а не его репозиторий), решение о статусе заявки и текст результата
 * возвращаются ядру — см. контракт {@link ApplicationHandler}.
 */
@Component
@RequiredArgsConstructor
public class RegisterWorkLicence implements ApplicationHandler {

    private final ChatUserService chatUserService;
    private final DocumentNumberGenerator documentNumberGenerator;

    @Override
    public ApplicationType getType() {
        return ApplicationType.REGISTER_WORK_LICENCE;
    }

    @Override
    public ApplicationDecision resolve(Application application) {
        ChatUser character = chatUserService.getChatUserById(application.getCharacterId());

        if (character.getWorkLicenceId() != null) {
            return ApplicationDecision.rejected(ApplicationMessageKey.WORK_LICENCE_ALREADY_EXISTS);
        }

        String workLicenceId = documentNumberGenerator.next(ApplicationType.REGISTER_WORK_LICENCE);
        chatUserService.issueWorkLicence(character.getId(), workLicenceId);

        // Профиль персонажа изменился — фронт должен перезагрузить currentChatUser.
        return ApplicationDecision.approved(
                ApplicationMessageKey.WORK_LICENCE_ISSUED,
                List.of(new CharacterUpdatedEvent(character.getId())),
                workLicenceId);
    }
}

