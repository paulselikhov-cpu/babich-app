package com.platform.chat.features.domainEvent.listeners;
import com.platform.chat.engines.domainEvent.events.CharacterUpdatedEvent;

import com.platform.chat.domain.chatUser.repository.ChatUserRepository;
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
public class CharacterUpdatedEventListener {

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

record CharacterUpdatedResponse(Long id) { }
