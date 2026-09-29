package com.platform.chat.core.modules.domainEvent.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.platform.chat.core.modules.domainEvent.DomainEvent;
import com.platform.chat.core.modules.domainEvent.EventType;
import com.platform.chat.core.modules.domainEvent.enums.ScopeType;

import java.util.Map;

/**
 * Событие «персонаж изменился». Публикуется там, где реально меняется
 * ChatUser (одобрение заявки, админская правка полей и т.п.).
 *
 * <p>Подписчик ({@code CharacterUpdatedEventListener}) шлёт WS-пуш владельцу
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

    /**
     * Creator для replay из {@code event_publication}: Jackson не наследует
     * {@code @JsonCreator} базового класса, поэтому у каждого факта объявлен
     * свой — он восстанавливает поля конверта (у этого события payload пуст).
     */
    @JsonCreator
    protected CharacterUpdatedEvent(@JsonProperty("type") EventType type,
                                    @JsonProperty("actorId") Long actorId,
                                    @JsonProperty("targetId") Long targetId,
                                    @JsonProperty("scopeType") ScopeType scopeType,
                                    @JsonProperty("scopeId") Long scopeId,
                                    @JsonProperty("payload") Map<String, Object> payload) {
        super(type, actorId, targetId, scopeType, scopeId, payload);
    }

    @JsonIgnore
    public Long getCharacterId() {
        return getTargetId();
    }
}
