package com.platform.chat.modules.notification.listener;

import com.platform.chat.core.event.childs.CharacterUpdatedEvent;
import com.platform.chat.modules.notification.dto.CharacterUpdatedResponse;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Слой 4 (Доставка). Реагирует на {@link CharacterUpdatedEvent} и шлёт WS-пуш
 * владельцу конкретного персонажа, чтобы тот перезагрузил {@code currentChatUser}
 * без F5 (паспорт/лицензия одобрены, админ поправил поля и т.п.).
 *
 * <p>Адресация — по {@code characterId}: пушим только тому пользователю, чей
 * персонаж реально изменился, а не всем подряд. Полный профиль фронт
 * перезапрашивает сам (см. CurrentChatUserService.refresh()), поэтому в payload
 * достаточно id.</p>
 */
@Component
@RequiredArgsConstructor
public class CharacterUpdatedListener {

    private final ChatUserRepository chatUserRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @ApplicationModuleListener
    public void onCharacterUpdated(CharacterUpdatedEvent event) {
        chatUserRepository.findById(event.getCharacterId())
                .ifPresent(character -> messagingTemplate.convertAndSendToUser(
                        character.getUsername(),
                        "/queue/character-updated",
                        new CharacterUpdatedResponse(character.getId())
                ));
    }
}