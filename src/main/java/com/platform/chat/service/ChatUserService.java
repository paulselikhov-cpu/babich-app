package com.platform.chat.service;

import com.platform.auth.repository.UserRepository;
import com.platform.chat.dto.CreateChatUserRequest;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.enums.ChatRole;
import com.platform.chat.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

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

    // получить персонажа по userId
    public ChatUser getChatUserByUserId(Long userId) {
        return chatUserRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Персонаж для userId не найден: " + userId));
    }

    // создать персонажа для уже существующего User
    @Transactional
    public ChatUser createChatUser(Long userId, CreateChatUserRequest request) {
        // проверяем что User существует
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + userId));

        // проверяем что у этого User ещё нет персонажа
        if (chatUserRepository.findByUserId(userId).isPresent()) {
            throw new RuntimeException("У пользователя уже есть персонаж");
        }

        // проверяем уникальность имени персонажа
        if (chatUserRepository.existsByCharacterName(request.getCharacterName())) {
            throw new RuntimeException("Имя персонажа уже занято");
        }

        ChatUser chatUser = new ChatUser();
        chatUser.setId(userId);                    // связываем с User через PK
        chatUser.setCharacterName(request.getCharacterName());
        chatUser.setGender(request.getGender());
        chatUser.setWeight(request.getWeight());
        chatUser.setHeight(request.getHeight());
        chatUser.setBadHabits(request.getBadHabits());
        chatUser.setRole(ChatRole.USER);            // по умолчанию USER
        chatUser.setRating(0);
        chatUser.setLastSeen(LocalDateTime.now());

        return chatUserRepository.save(chatUser);
    }

    // обновить last_seen — будем вызывать при каждом подключении к WebSocket
    @Transactional
    public void updateLastSeen(Long userId) {
        chatUserRepository.findByUserId(userId).ifPresent(chatUser -> {
            chatUser.setLastSeen(LocalDateTime.now());
            chatUserRepository.save(chatUser);
        });
    }

    // топ персонажей по рейтингу
    public List<ChatUser> getTopChatUsers() {
        return chatUserRepository.findAllByOrderByRatingDesc();
    }

    // обновить рейтинг
    @Transactional
    public ChatUser updateRating(Long userId, Integer delta) {
        ChatUser chatUser = getChatUserByUserId(userId);
        chatUser.setRating(chatUser.getRating() + delta);
        return chatUserRepository.save(chatUser);
    }
}