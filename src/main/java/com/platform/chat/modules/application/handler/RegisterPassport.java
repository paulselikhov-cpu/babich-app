package com.platform.chat.modules.application.handler;
import com.platform.chat.modules.application.ApplicationType;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;

import com.platform.chat.core.modules.application.models.ApplicationHandler;
import com.platform.chat.core.modules.application.entity.Application;
import com.platform.chat.core.modules.application.enums.ApplicationMessageKey;
import com.platform.chat.core.modules.application.models.ApplicationStatus;
import com.platform.chat.core.modules.application.utils.ApplicationIdGenerator;
import com.platform.chat.core.modules.application.utils.ApplicationMessages;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegisterPassport implements ApplicationHandler {
    private final ChatUserRepository chatUserRepository;
    private final ApplicationMessages messages;

    @Override
    public ApplicationType getType() {
        return ApplicationType.REGISTER_PASSPORT;
    }

    @Override
    public void handle(Application application) {
        ChatUser user = chatUserRepository.findById(application.getCharacterId())
                .orElseThrow(() -> new RuntimeException(
                        messages.resolve(ApplicationMessageKey.CHARACTER_NOT_FOUND, application.getCharacterId())
                ));

        if (user.getPassportId() != null) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setResultMessage(messages.resolve(ApplicationMessageKey.PASSPORT_ALREADY_EXISTS));
            return;
        }

        String passportId = ApplicationIdGenerator.generate(application.getType());
        user.setPassportId(passportId);
        user.setGameStatus("RESIDENT");
        chatUserRepository.save(user);

        application.setStatus(ApplicationStatus.APPROVED);
        application.setResultMessage(messages.resolve(ApplicationMessageKey.PASSPORT_ISSUED, passportId));
    }
}
