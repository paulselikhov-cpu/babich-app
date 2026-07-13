package com.platform.chat.service.message;

import com.platform.chat.dto.response.ChatMessageResponse;
import com.platform.chat.dto.response.RoomHistoryResponse;
import com.platform.chat.entity.Message;
import com.platform.chat.entity.Room;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.entity.RoomReadStatus;
import com.platform.chat.enums.LocationMemberRole;
import com.platform.chat.enums.MessageType;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.repository.location.LocationMemberRepository;
import com.platform.chat.repository.message.MessageRepository;
import com.platform.chat.repository.room.RoomReadStatusRepository;
import com.platform.chat.repository.room.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomReadStatusRepository readStatusRepository;

    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);
    /**
     * Сохраняет сообщение в БД и обогащает ChatMessage:
     * - устанавливает id из БД
     * - устанавливает character (userId, characterName, avatarUrl)
     *
     * Вызывается только для MessageType.CHAT.
     */
    @Transactional
    public Long saveByUser(ChatUser chatUser, String content, String stringRoomId) {
        Long roomId = Long.parseLong(stringRoomId);
        Room room = roomRepository.findById(roomId).orElse(null);

        if (room == null) {
            log.debug("Комната с id {} не найдена в системе", roomId);
            return null;
        }

        Message saved = saveMessage(room, chatUser, content);

        log.debug("Сообщение {} сохранено в комнате {}", saved.getId(), roomId);
        return saved.getId();
    }

    public Message saveMessage(Room room, ChatUser chatUser, String content) {
        Message message = new Message();
        message.setRoom(room);
        message.setCharacter(chatUser);
        message.setContent(content);
        message.setType(MessageType.CHAT);
        message.setIsDeleted(false);
        return messageRepository.save(message);
    }

    /**
     * Загружает историю И атомарно помечает комнату прочитанной — в ОДНОЙ
     * транзакции, чтобы не зависеть от порядка REST-запроса и WS-события
     * входа в комнату (они сейчас гоняются гонкой на фронте).
     *
     * Порядок действий важен:
     *  1) сначала читаем СТАРУЮ метку lastReadAt (что было непрочитано ДО этого визита)
     *  2) считаем по ней firstUnreadMessageId/unreadCount
     *  3) только теперь обновляем метку на "сейчас"
     * Если поменять порядок — потеряем возможность узнать, что было непрочитанным.
     */
    @Transactional
    public RoomHistoryResponse getHistoryAndMarkRead(Long roomId, Long characterId, int page, int size) {
        LocalDateTime previousReadAt = readStatusRepository.findByCharacterIdAndRoomId(characterId, roomId)
                .map(RoomReadStatus::getLastReadAt)
                .orElse(EPOCH); // ни разу не заходил — считаем непрочитанным всё

        Page<Message> result = messageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId, PageRequest.of(page, size)
        );
        List<Message> messages = new ArrayList<>(result.getContent());
        Collections.reverse(messages);

        // первое сообщение НЕ от самого пользователя, написанное после его последнего визита
        Long firstUnreadMessageId = messages.stream()
                .filter(m -> m.getCreatedAt().isAfter(previousReadAt))
                .filter(m -> !m.getCharacter().getId().equals(characterId))
                .map(Message::getId)
                .findFirst()
                .orElse(null);

        long unreadCount = messages.stream()
                .filter(m -> m.getCreatedAt().isAfter(previousReadAt))
                .filter(m -> !m.getCharacter().getId().equals(characterId))
                .count();

        // теперь, когда посчитали разницу, обновляем метку — комната официально прочитана
        markRead(characterId, roomId);

        List<ChatMessageResponse> dos = messages.stream()
                .map(ChatMessageResponse::getResponseForHistory)
                .collect(Collectors.toList());

        return new RoomHistoryResponse(dos, firstUnreadMessageId, unreadCount);
    }

    private void markRead(Long characterId, Long roomId) {
        RoomReadStatus status = readStatusRepository.findByCharacterIdAndRoomId(characterId, roomId)
                .orElseGet(() -> {
                    RoomReadStatus s = new RoomReadStatus();
                    s.setCharacter(chatUserRepository.getReferenceById(characterId));
                    s.setRoom(roomRepository.getReferenceById(roomId));
                    return s;
                });
        status.setLastReadAt(LocalDateTime.now());
        readStatusRepository.save(status);
    }

    /**
     * Загрузить историю сообщений комнаты с пагинацией.
     * Возвращает последние N сообщений отсортированных по времени.
     *
     * @param roomId id комнаты
     * @param page   номер страницы (0 = последние сообщения)
     * @param size   количество сообщений на странице
     */
    public List<ChatMessageResponse> getHistory(Long roomId, int page, int size) {
        Page<Message> result = messageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId,
                PageRequest.of(page, size)
        );
        List<Message> messages = new ArrayList<>(result.getContent());
        Collections.reverse(messages);

        // конвертируем в DTO
        return messages.stream()
                .map(ChatMessageResponse::getResponseForHistory)
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

        boolean isAuthor = message.getCharacter() != null &&
                message.getCharacter().getId().equals(character.getId());

        if (!isAuthor) {
            var member = message.getRoom().getLocation() != null
                    ? locationMemberRepository.findByLocationIdAndCharacterId(
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

        // уведомляем всех в комнате об удалении
        Long roomId = message.getRoom().getId();
        messagingTemplate.convertAndSend(
                "/topic/room." + roomId,
                ChatMessageResponse.deleted(messageId, roomId)
        );
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