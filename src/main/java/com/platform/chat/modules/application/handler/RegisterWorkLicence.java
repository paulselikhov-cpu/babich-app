package com.platform.chat.modules.application.handler;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.modules.application.entity.Application;
import com.platform.chat.modules.application.enums.ApplicationMessageKey;
import com.platform.chat.modules.application.enums.ApplicationStatus;
import com.platform.chat.modules.application.enums.ApplicationType;
import com.platform.chat.modules.application.utils.ApplicationIdGenerator;
import com.platform.chat.modules.application.utils.ApplicationMessages;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

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
        List<ChatUser> users = chatUserRepository.findAllByUserId(application.getUserId());
        if (users.isEmpty()) {
            throw new RuntimeException(
                    messages.resolve(ApplicationMessageKey.CHARACTER_NOT_FOUND, application.getUserId())
            );
        }
        ChatUser user = users.getFirst();

        if (user.getWorkLicenceId() != null) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setResultMessage(messages.resolve(ApplicationMessageKey.WORK_LICENCE_ALREADY_EXISTS));
            return;
        }

        String workLicenceId = ApplicationIdGenerator.generate(application.getType());
        user.setWorkLicenceId(workLicenceId);

        application.setStatus(ApplicationStatus.APPROVED);
        application.setResultMessage(messages.resolve(ApplicationMessageKey.WORK_LICENCE_ISSUED, workLicenceId));
    }
}
