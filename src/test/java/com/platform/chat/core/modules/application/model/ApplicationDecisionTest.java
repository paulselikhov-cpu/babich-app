package com.platform.chat.core.modules.application.model;

import com.platform.chat.core.modules.application.enums.ApplicationMessageKey;
import com.platform.chat.core.modules.application.enums.ApplicationStatus;
import com.platform.chat.modules.domainEvent.events.CharacterUpdatedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Регрессия на перегрузки {@link ApplicationDecision}: вызов
 * {@code approved(key, List.of(new CharacterUpdatedEvent(id)), arg)} обязан
 * попасть в ветку «решение + события», а не в ветку {@code Object...}
 * (иначе события молча уехали бы в аргументы текста и не публиковались).
 */
class ApplicationDecisionTest {

    @Test
    void approvedWithEventsKeepsEventsOutOfMessageArgs() {
        ApplicationDecision decision = ApplicationDecision.approved(
                ApplicationMessageKey.PASSPORT_ISSUED,
                List.of(new CharacterUpdatedEvent(42L)),
                "PP-1111-2222");

        assertThat(decision.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(decision.events()).hasSize(1);
        assertThat(decision.events().get(0)).isInstanceOf(CharacterUpdatedEvent.class);
        assertThat(((CharacterUpdatedEvent) decision.events().get(0)).getCharacterId()).isEqualTo(42L);
        assertThat(decision.args()).containsExactly("PP-1111-2222");
    }

    @Test
    void approvedWithoutEventsHasNoEffects() {
        ApplicationDecision decision = ApplicationDecision.approved(
                ApplicationMessageKey.PASSPORT_ISSUED, "PP-1111-2222");

        assertThat(decision.events()).isEmpty();
        assertThat(decision.args()).containsExactly("PP-1111-2222");
    }

    @Test
    void rejectedHasStatusAndNoEffects() {
        ApplicationDecision decision = ApplicationDecision.rejected(
                ApplicationMessageKey.PASSPORT_ALREADY_EXISTS);

        assertThat(decision.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(decision.args()).isEmpty();
        assertThat(decision.events()).isEmpty();
    }
}
