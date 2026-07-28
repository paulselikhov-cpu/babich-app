package com.platform.chat.service.chatUser;

import com.platform.auth.repository.UserRepository;
import com.platform.chat.dto.request.CreateChatUserRequest;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.enums.ChatRole;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUserService {

    private final ChatUserRepository chatUserRepository;
    private final UserRepository userRepository;

    // получить всех персонажей
    public List<ChatUser> getAllChatUsers() {
        return chatUserRepository.findAll();
    }

    // получить персонажа по id
    public ChatUser getChatUserById(Long id) {
        return chatUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + id));
    }

    // получить всех персонажей пользователя
    public List<ChatUser> getAllChatUsersByUserId(Long userId) {
        return chatUserRepository.findAllByUserId(userId);
    }

    // получить персонажа пользователя в конкретном районе
    public ChatUser getChatUserByUserIdAndDistrictId(Long userId, Long districtId) {
        return chatUserRepository.findByUserIdAndDistrictId(userId, districtId)
                .orElseThrow(() -> new RuntimeException(
                        "Персонаж для userId=" + userId + " в районе " + districtId + " не найден"));
    }

    // получить персонажа пользователя в районе (Optional)
    public Optional<ChatUser> findChatUserByUserIdAndDistrictId(Long userId, Long districtId) {
        return chatUserRepository.findByUserIdAndDistrictId(userId, districtId);
    }

    // получить персонажа по userId (Optional) — для обратной совместимости,
    // ищет среди всех персонажей пользователя, возвращает первого найденного
    @Deprecated
    public Optional<ChatUser> findChatUserByUserId(Long userId) {
        List<ChatUser> users = chatUserRepository.findAllByUserId(userId);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }

    // получить персонажа по userId (выбрасывает исключение если нет) — для обратной совместимости
    @Deprecated
    public ChatUser getChatUserByUserId(Long userId) {
        return findChatUserByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Персонаж для userId не найден: " + userId));
    }

    public ChatUser getChatUserByUserName(String userName) {
        ChatUser chatUser = chatUserRepository.findByUsername(userName).orElse(null);
        if (chatUser == null) {
            log.debug("Пользователь {} не найден в системе", userName);
            return null;
        }
        return chatUser;
    }

    // создать персонажа для уже существующего User в указанном районе
    @Transactional
    public ChatUser createChatUser(Long userId, CreateChatUserRequest request) {
        // проверяем что User существует
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + userId));

        // проверяем что у этого User ещё нет персонажа в данном районе
        if (chatUserRepository.existsByUserIdAndDistrictId(userId, request.getDistrictId())) {
            throw new RuntimeException(
                    "У пользователя уже есть персонаж в районе " + request.getDistrictId());
        }

        // проверяем уникальность имени персонажа в рамках района
        if (chatUserRepository.existsByCharacterNameAndDistrictId(
                request.getCharacterName(), request.getDistrictId())) {
            throw new RuntimeException(
                    "Имя персонажа уже занято в районе " + request.getDistrictId());
        }

        // получаем username из User
        String username = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + userId))
                .getUsername();

        ChatUser chatUser = new ChatUser();
        chatUser.setUserId(userId);                 // связываем с User по userId
        chatUser.setUsername(username);             // дублируем для WS-уведомлений
        chatUser.setCharacterName(request.getCharacterName());
        chatUser.setDistrictId(request.getDistrictId());
        chatUser.setGender(request.getGender());
        chatUser.setWeight(request.getWeight());
        chatUser.setHeight(request.getHeight());
        chatUser.setBadHabits(request.getBadHabits());
        chatUser.setAvatarUrl(request.getAvatarUrl());
        chatUser.setRole(ChatRole.USER);            // по умолчанию USER
        chatUser.setRating(0);
        chatUser.setCoinBalance(0L);
        chatUser.setXpUser(0L);
        chatUser.setEnergy(100);
        chatUser.setGameStatus("HOMELESS");
        chatUser.setProfession(com.platform.economy.enums.Profession.DVORNIK);
        chatUser.setLastSeen(LocalDateTime.now());
        chatUser.setEnergyLastUpdated(LocalDateTime.now());

        return chatUserRepository.save(chatUser);
    }

    // обновить last_seen для персонажа в районе
    @Transactional
    public void updateLastSeen(Long userId, Long districtId) {
        chatUserRepository.findByUserIdAndDistrictId(userId, districtId).ifPresent(chatUser -> {
            chatUser.setLastSeen(LocalDateTime.now());
            chatUserRepository.save(chatUser);
        });
    }

    // обновить last_seen для любого персонажа пользователя (для обратной совместимости)
    @Deprecated
    @Transactional
    public void updateLastSeen(Long userId) {
        findChatUserByUserId(userId).ifPresent(chatUser -> {
            chatUser.setLastSeen(LocalDateTime.now());
            chatUserRepository.save(chatUser);
        });
    }

    // топ персонажей по рейтингу
    public List<ChatUser> getTopChatUsers() {
        return chatUserRepository.findAllByOrderByRatingDesc();
    }

    // обновить рейтинг персонажа в районе
    @Transactional
    public ChatUser updateRating(Long userId, Long districtId, Integer delta) {
        ChatUser chatUser = getChatUserByUserIdAndDistrictId(userId, districtId);
        chatUser.setRating(chatUser.getRating() + delta);
        return chatUserRepository.save(chatUser);
    }

    // обновить рейтинг (для обратной совместимости)
    @Deprecated
    @Transactional
    public ChatUser updateRating(Long userId, Integer delta) {
        ChatUser chatUser = getChatUserByUserId(userId);
        chatUser.setRating(chatUser.getRating() + delta);
        return chatUserRepository.save(chatUser);
    }
}