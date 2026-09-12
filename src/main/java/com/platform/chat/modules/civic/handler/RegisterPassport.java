package com.platform.chat.modules.civic.handler;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.modules.civic.entity.Application;
import com.platform.chat.modules.civic.enums.ApplicationStatus;
import com.platform.chat.modules.civic.enums.ApplicationType;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RegisterPassport implements ApplicationHandler {
    private final ChatUserRepository chatUserRepository;

    @Override
    public ApplicationType getType() {
        return ApplicationType.REGISTER_PASSPORT;
    }

    @Override
    public void handle(Application application) {
        List<ChatUser> users = chatUserRepository.findAllByUserId(application.getUserId());
        if (users.isEmpty()) {
            throw new RuntimeException("Персонаж не найден для userId: " + application.getUserId());
        }
        ChatUser user = users.getFirst();

        if (user.getPassportId() != null) {
            application.setStatus(ApplicationStatus.REJECTED);
            application.setResultMessage("У персонажа уже есть паспорт");
            return;
        }

        String passportId = generatePassportId();
        user.setPassportId(passportId);
        user.setGameStatus("RESIDENT");

        application.setStatus(ApplicationStatus.APPROVED);
        application.setResultMessage("Паспорт выдан: " + passportId);
    }

    private static String generatePassportId() {
        SecureRandom random = new SecureRandom();
        int first = random.nextInt(9000) + 1000; // 1000..9999
        int second = random.nextInt(9000) + 1000; // 1000..9999
        return String.format("PP-%04d-%04d", first, second);
    }
}
