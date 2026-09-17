package com.platform.chat.service.message;

import com.platform.chat.dto.response.ChatMessageResponse;
import com.platform.chat.dto.response.RoomHistoryResponse;
import com.platform.chat.entity.Message;
import com.platform.chat.entity.Room;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.entity.RoomReadStatus;
import com.platform.chat.enums.LocationUserRole;
import com.platform.chat.enums.MessageType;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.repository.location.LocationUserRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final ChatUserRepository chatUserRepository;
    private final LocationUserRepository locationUserRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomReadStatusRepository readStatusRepository;
    private final UnreadService unreadService;

    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);

    /**
     * Сохраняет сообщение и сразу возвращает готовый DTO (собранный внутри
     * этой же транзакции — иначе лениво подгружаемый replyTo.character
     * упадёт с LazyInitializationException после выхода из метода).
     * replyToId — опционален. Если указан, но сообщение не найдено или
     * принадлежит другой комнате — реплай молча игнорируется (не кидаем
     * ошибку, чтобы не ронять отправку из-за гонки с удалением/подменой).
     */
    @Transactional
    public ChatMessageResponse saveByUser(ChatUser chatUser, String content, String stringRoomId, Long replyToId) {
        Long roomId = Long.parseLong(stringRoomId);
        Room room = roomRepository.findById(roomId).orElse(null);

        if (room == null) {
            log.debug("Комната с id {} не найдена в системе", roomId);
            return null;
        }

        Message replyTo = resolveReplyTarget(replyToId, roomId);

        Message saved = saveMessage(room, chatUser, content, replyTo);
        log.debug("Сообщение {} сохранено в комнате {}", saved.getId(), roomId);

        return ChatMessageResponse.fromMessage(saved);
    }

    private Message resolveReplyTarget(Long replyToId, Long roomId) {
        if (replyToId == null) return null;

        Message replyTo = messageRepository.findById(replyToId).orElse(null);
        if (replyTo == null) {
            log.debug("Сообщение-родитель {} для реплая не найдено", replyToId);
            return null;
        }
        // защита от кросс-комнатного реплая (например, id подменили на фронте)
        if (!replyTo.getRoom().getId().equals(roomId)) {
            log.warn("Попытка реплая на сообщение {} из другой комнаты", replyToId);
            return null;
        }
        return replyTo;
    }

    public Message saveMessage(Room room, ChatUser chatUser, String content, Message replyTo) {
        Message message = new Message();
        message.setRoom(room);
        message.setCharacter(chatUser);
        message.setContent(content);
        message.setType(MessageType.CHAT);
        message.setIsDeleted(false);
        message.setReplyTo(replyTo);
        return messageRepository.save(message);
    }

    @Transactional
    public RoomHistoryResponse getHistoryAndMarkRead(Long roomId, Long characterId, int page, int size) {
        LocalDateTime previousReadAt = readStatusRepository.findByCharacterIdAndRoomId(characterId, roomId)
                .map(RoomReadStatus::getLastReadAt)
                .orElse(EPOCH);

        Page<Message> result = messageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId, PageRequest.of(page, size)
        );
        List<Message> messages = new ArrayList<>(result.getContent());
        Collections.reverse(messages);

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

        unreadService.markRead(characterId, roomId);

        Room room = roomRepository.findById(roomId).orElse(null);
        if (room != null) {
            ChatUser character = chatUserRepository.findById(characterId).orElse(null);
            if (character != null) {
                unreadService.notifySelfRoomRead(room, characterId, character.getUsername());
            }
        }

        List<ChatMessageResponse> dos = messages.stream()
                .map(ChatMessageResponse::fromMessage)
                .collect(Collectors.toList());

        return new RoomHistoryResponse(dos, firstUnreadMessageId, unreadCount);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getHistory(Long roomId, int page, int size) {
        Page<Message> result = messageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId,
                PageRequest.of(page, size)
        );
        List<Message> messages = new ArrayList<>(result.getContent());
        Collections.reverse(messages);

        return messages.stream()
                .map(ChatMessageResponse::fromMessage)
                .collect(Collectors.toList());
    }

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
                    ? locationUserRepository.findByLocationIdAndCharacterId(
                    message.getRoom().getLocation().getId(),
                    character.getId()
            ).orElse(null)
                    : null;

            boolean isModerator = member != null &&
                    (member.getRole() == LocationUserRole.OWNER ||
                            member.getRole() == LocationUserRole.MODERATOR);

            if (!isModerator) {
                throw new RuntimeException("Нет прав для удаления сообщения");
            }
        }

        message.setIsDeleted(true);
        messageRepository.save(message);

        Long roomId = message.getRoom().getId();
        messagingTemplate.convertAndSend(
                "/topic/room." + roomId,
                ChatMessageResponse.deleted(messageId, roomId)
        );
    }

    @Transactional
    public void deleteMessages(List<Long> messageIds, String username) {
        messageIds.forEach(id -> deleteMessage(id, username));
    }
}