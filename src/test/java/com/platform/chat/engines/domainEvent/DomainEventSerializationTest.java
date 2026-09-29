package com.platform.chat.engines.domainEvent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.chat.engines.domainEvent.events.ApplicationResolvedEvent;
import com.platform.chat.engines.domainEvent.events.CharacterUpdatedEvent;
import com.platform.chat.engines.domainEvent.events.CoinsChangedEvent;
import com.platform.chat.engines.domainEvent.events.ElectionClosedEvent;
import com.platform.chat.engines.domainEvent.events.ElectionStartedEvent;
import com.platform.chat.engines.domainEvent.events.PollClosedEvent;
import com.platform.chat.engines.domainEvent.events.XpChangedEvent;
import com.platform.chat.engines.transactionLog.enums.CoinUpdateReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Контракт сериализации ядра событий.
 *
 * <p>Modulith пишет каждое событие в {@code event_publication} в транзакции
 * публикатора и при падении процесса между коммитом и обработкой переигрывает
 * его после рестарта: JSON десериализуется <b>по базовому типу</b>
 * {@link DomainEvent} — подтип определяется по логическому имени из
 * {@code @JsonTypeInfo}/{@code @JsonSubTypes} (не по FQCN). Тест-фиксация этого
 * контракта нужна в том числе потому, что события живут в одном слайсе с
 * конвертом ({@code core.modules.domainEvent}): переезд событий в другой пакет
 * не должен менять формат хранения.</p>
 *
 * <p>Второй тест — про физический лимит: колонка
 * {@code event_publication.serialized_event} объявлена как
 * {@code varchar(255)}, поэтому самое «длинное» событие обязано влезать.</p>
 */
class DomainEventSerializationTest {

    /** Лимит колонки {@code event_publication.serialized_event} (varchar(255)). */
    private static final int SERIALIZED_EVENT_LIMIT = 255;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Полный словарь фактов ядра — по одному экземпляру каждого события.
     * Новое событие обязано попасть и сюда, и в {@code @JsonSubTypes}.
     */
    private static List<DomainEvent> allEvents() {
        return List.of(
                new ApplicationResolvedEvent(1L, 2L, 3L, "REGISTER_WORK_LICENCE", "APPROVED"),
                new CharacterUpdatedEvent(2L),
                new CoinsChangedEvent(2L, 100L, CoinUpdateReason.WORK_SHIFT, 7L),
                new XpChangedEvent(2L, 5L, CoinUpdateReason.WORK_SHIFT, null),
                new ElectionStartedEvent(1L, 10L, 5L),
                new ElectionClosedEvent(1L, 10L, "Вася"),
                new PollClosedEvent(1L, 10L, "ELECTION", "DISTRICT")
        );
    }

    @Test
    @DisplayName("Каждое событие переживает JSON round-trip по базовому типу DomainEvent (replay)")
    void everyEventSurvivesRoundTripAsBaseType() throws Exception {
        for (DomainEvent event : allEvents()) {
            String json = mapper.writeValueAsString(event);

            DomainEvent restored = mapper.readValue(json, DomainEvent.class);

            String name = event.getClass().getSimpleName();
            assertThat(restored).as("тип после replay: %s", name).isInstanceOf(event.getClass());
            assertThat(restored.getType()).as("EventType: %s", name).isEqualTo(event.getType());
            assertThat(restored.getActorId()).as("actorId: %s", name).isEqualTo(event.getActorId());
            assertThat(restored.getTargetId()).as("targetId: %s", name).isEqualTo(event.getTargetId());
            assertThat(restored.getScopeId()).as("scopeId: %s", name).isEqualTo(event.getScopeId());
            assertThat(restored.getScopeType()).as("scopeType: %s", name).isEqualTo(event.getScopeType());
            assertPayloadRestored(event.getPayload(), restored.getPayload(), name);
        }
    }

    @Test
    @DisplayName("После replay вычисляемые геттеры читаются без ClassCastException (числа payload — Integer)")
    void replayedEventGettersWork() throws Exception {
        CoinsChangedEvent original = new CoinsChangedEvent(2L, 100L, CoinUpdateReason.WORK_SHIFT, 7L);

        DomainEvent restored = mapper.readValue(mapper.writeValueAsString(original), DomainEvent.class);

        CoinsChangedEvent replayed = (CoinsChangedEvent) restored;
        assertThat(replayed.getCharacterId()).isEqualTo(2L);
        assertThat(replayed.getAmount()).isEqualTo(100L);
        assertThat(replayed.getRelatedEntityId()).isEqualTo(7L);
        assertThat(replayed.getReason()).isEqualTo(CoinUpdateReason.WORK_SHIFT);
    }

    @Test
    @DisplayName("Сериализованное событие влезает в event_publication.serialized_event (varchar(255))")
    void serializedEventsFitVarchar255() throws Exception {
        for (DomainEvent event : allEvents()) {
            String json = mapper.writeValueAsString(event);

            assertThat(json.length())
                    .as("длина JSON события %s", event.getClass().getSimpleName())
                    .isLessThanOrEqualTo(SERIALIZED_EVENT_LIMIT);
        }
    }

    /**
     * Сравнивает payload с оглядкой на то, как Jackson восстанавливает
     * числа из JSON без типовой информации: {@code Long} приходит обратно
     * {@code Integer}. Значения сверяем, тип — нет (иначе тест фиксировал бы
     * особенность Jackson, а не контракт события).
     */
    private static void assertPayloadRestored(Map<String, Object> expected,
                                              Map<String, Object> restored,
                                              String eventName) {
        if (expected == null) {
            assertThat(restored).as("payload: %s", eventName).isNull();
            return;
        }
        assertThat(restored).as("payload: %s", eventName).isNotNull();
        assertThat(restored.keySet()).as("ключи payload: %s", eventName).isEqualTo(expected.keySet());

        expected.forEach((key, value) -> {
            Object actual = restored.get(key);
            if (value instanceof Number expectedNumber && actual instanceof Number actualNumber) {
                assertThat(actualNumber.longValue())
                        .as("payload[%s] события %s", key, eventName)
                        .isEqualTo(expectedNumber.longValue());
            } else {
                assertThat(actual).as("payload[%s] события %s", key, eventName).isEqualTo(value);
            }
        });
    }
}
