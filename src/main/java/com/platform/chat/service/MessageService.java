package com.platform.chat.service;

import com.platform.chat.dto.ChatMessage;
import com.platform.chat.dto.MessageResponse;
import com.platform.chat.entity.Message;
import com.platform.chat.entity.Room;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.enums.LocationMemberRole;
import com.platform.chat.repository.LocationMemberRepository;
import com.platform.chat.enums.MessageType;
import com.platform.chat.repository.MessageRepository;
import com.platform.chat.repository.RoomRepository;
import com.platform.chat.repository.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Сервис для работы с сообщениями чата.
 * Сохраняет сообщения в БД только если Room.storeHistory = true.
 * При storeHistory = false — сообщение летит только через WebSocket.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final ChatUserRepository chatUserRepository;
    private final LocationMemberRepository locationMemberRepository;

    /**
     * Сохранить сообщение в БД.
     * Вызывается из ChatController только для CHAT типа.
     * JOIN/LEAVE не сохраняются — они временные события.
     *
     * @param chatMessage DTO из WebSocket
     * @param username    имя пользователя из JWT (Principal.getName())
     */
    @Transactional
    public void saveMessage(ChatMessage chatMessage, String username) {
        // находим комнату
        Long roomId = Long.parseLong(chatMessage.getRoomId());
        Room room = roomRepository.findById(roomId).orElse(null);

        if (room == null) {
            log.warn("Комната не найдена: {}", roomId);
            return;
        }

        // проверяем флаг хранения истории
        if (!room.getStoreHistory()) {
            log.debug("Комната {} не хранит историю — пропускаем сохранение", roomId);
            return;
        }

        // находим персонажа по username
        // username из JWT → users.username → chat_users
        ChatUser character = chatUserRepository
                .findByUsername(username)
                .orElse(null);

        if (character == null) {
            log.warn("Персонаж не найден для username: {}", username);
            return;
        }

        // создаём и сохраняем сообщение
        Message message = new Message();
        message.setRoom(room);
        message.setCharacter(character);
        message.setContent(chatMessage.getContent());
        message.setType(MessageType.CHAT);
        message.setIsDeleted(false);

        messageRepository.save(message);
        log.debug("Сообщение сохранено в комнате {}", roomId);
    }

    /**
     * Загрузить историю сообщений комнаты с пагинацией.
     * Возвращает последние N сообщений отсортированных по времени.
     *
     * @param roomId id комнаты
     * @param page   номер страницы (0 = последние сообщения)
     * @param size   количество сообщений на странице
     */
    public List<MessageResponse> getHistory(Long roomId, int page, int size) {
        Page<Message> result = messageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId,
                PageRequest.of(page, size)
        );
        List<Message> messages = new ArrayList<>(result.getContent());
        Collections.reverse(messages);

        // конвертируем в DTO
        return messages.stream()
                .map(MessageResponse::from)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Мягкое удаление одного сообщения.
     * Только автор или модератор/владелец локации может удалять.
     * soft delete — запись остаётся в БД, isDeleted = true.
     */
    @Transactional
    public void deleteMessage(Long messageId, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Сообщение не найдено"));

        ChatUser character = chatUserRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден"));

        // проверяем права: автор или модератор/владелец локации
        boolean isAuthor = message.getCharacter() != null &&
                message.getCharacter().getId().equals(character.getId());

        if (!isAuthor) {
            // проверяем роль в локации
            var member = message.getRoom().getLocation() != null
                    ? locationMemberRepository
                    .findByLocationIdAndCharacterId(
                            message.getRoom().getLocation().getId(),
                            character.getId()
                    ).orElse(null)
                    : null;

            boolean isModerator = member != null &&
                    (member.getRole() == LocationMemberRole.OWNER ||
                            member.getRole() == LocationMemberRole.MODERATOR);

            if (!isModerator) {
                throw new RuntimeException("Нет прав для удаления сообщения");
            }
        }

        message.setIsDeleted(true);
        messageRepository.save(message);
    }

    /**
     * Удалить несколько сообщений сразу.
     * Используется при групповом удалении выделенных сообщений.
     */
    @Transactional
    public void deleteMessages(List<Long> messageIds, String username) {
        messageIds.forEach(id -> deleteMessage(id, username));
    }
}