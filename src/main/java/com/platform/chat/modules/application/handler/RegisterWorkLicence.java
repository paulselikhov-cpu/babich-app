package com.platform.chat.modules.application.handler;

import com.platform.chat.core.modules.application.models.ApplicationHandler;
import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.core.modules.application.entity.Application;
import com.platform.chat.core.modules.application.enums.ApplicationMessageKey;
import com.platform.chat.core.modules.application.models.ApplicationStatus;
import com.platform.chat.core.modules.application.ApplicationType;
import com.platform.chat.core.modules.application.utils.ApplicationIdGenerator;
import com.platform.chat.core.modules.application.utils.ApplicationMessages;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegisterWorkLicence implements ApplicationHandler {
    private final ChatUserRepository chatUserRepository;
    private final ApplicationMessages messages;

    @Override
    public ApplicationType getType() {
        return ApplicationType.REGISTER_WORK_LICENCE;
    }

    @Override
    public void handle(Application application) {
        ChatUser user = chatUserRepository.findById(application.getCharacterId())
                .orElseThrow(() -> new RuntimeException(
                        messages.resolve(ApplicationMessageKey.CHARACTER_NOT_FOUND, application.getCharacterId())
                ));

        if (user.getWorkLicenceId() != null) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setResultMessage(messages.resolve(ApplicationMessageKey.WORK_LICENCE_ALREADY_EXISTS));
            return;
        }

        String workLicenceId = ApplicationIdGenerator.generate(application.getType());
        user.setWorkLicenceId(workLicenceId);
        chatUserRepository.save(user);

        application.setStatus(ApplicationStatus.APPROVED);
        application.setResultMessage(messages.resolve(ApplicationMessageKey.WORK_LICENCE_ISSUED, workLicenceId));
    }
}
