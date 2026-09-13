package com.platform.chat.modules.application.handler;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.modules.application.entity.Application;
import com.platform.chat.modules.application.enums.ApplicationStatus;
import com.platform.chat.modules.application.enums.ApplicationType;
import com.platform.chat.modules.application.utils.ApplicationIdGenerator;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RegisterWorkLicence implements ApplicationHandler {
    private final ChatUserRepository chatUserRepository;

    @Override
    public ApplicationType getType() {
        return ApplicationType.REGISTER_WORK_LICENCE;
    }

    @Override
    public void handle(Application application) {
        List<ChatUser> users = chatUserRepository.findAllByUserId(application.getUserId());
        if (users.isEmpty()) {
            throw new RuntimeException("Персонаж не найден для userId: " + application.getUserId());
        }
        ChatUser user = users.getFirst();

        if (user.getWorkLicenceId() != null) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setResultMessage("У персонажа уже есть лицензия для работы");
            return;
        }

        String workLicenceId = ApplicationIdGenerator.generate(application.getType());
        user.setWorkLicenceId(workLicenceId);

        application.setStatus(ApplicationStatus.APPROVED);
        application.setResultMessage("Лицензия для работу выдана");
    }

    private static String generateId(ApplicationType type) {
        SecureRandom random = new SecureRandom();
        int first = random.nextInt(9000) + 1000; // 1000..9999
        int second = random.nextInt(9000) + 1000; // 1000..9999

        return switch (type) {
            case REGISTER_WORK_LICENCE -> String.format("WRK-%04d-%04d", first, second);
            case REGISTER_PASSPORT -> String.format("PP-%04d-%04d", first, second);
            case null, default -> String.format("DF-%04d-%04d", first, second);
        };

    }
}
