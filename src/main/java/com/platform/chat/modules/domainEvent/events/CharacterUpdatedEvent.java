package com.platform.chat.modules.domainEvent.events;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.core.modules.domainEvent.DomainEvent;
import com.platform.chat.core.modules.domainEvent.EventType;
import com.platform.chat.core.modules.domainEvent.enums.ScopeType;

/**
 * Событие «персонаж изменился». Публикуется там, где реально меняется
 * ChatUser (одобрение заявки, админская правка полей и т.п.).
 *
 * <p>Подписчик ({@code CharacterUpdatedListener}) шлёт WS-пуш владельцу
 * конкретного персонажа в {@code /user/{username}/queue/character-updated},
 * чтобы фронт перезагрузил {@code currentChatUser} без F5.</p>
 *
 * <p>Адресация — по внутреннему {@code characterId} (ChatUser.id), а не по
 * внешнему User.id: у пользователя может быть несколько персонажей (по одному
 * на район), и обновляться должен только изменённый. В payload данных нет —
 * слушателю достаточно id, полный профиль фронт получает через refresh().</p>
 */
public class CharacterUpdatedEvent extends DomainEvent {

    /**
     * @param characterId внутренний id персонажа (ChatUser.id), который изменился
     */
    public CharacterUpdatedEvent(Long characterId) {
        super(
                EventType.CHARACTER_UPDATED,
                characterId, // actorId
                characterId, // targetId
                ScopeType.CHARACTER,
                characterId  // scopeId
        );
    }

    @JsonIgnore
    public Long getCharacterId() {
        return getTargetId();
    }
}