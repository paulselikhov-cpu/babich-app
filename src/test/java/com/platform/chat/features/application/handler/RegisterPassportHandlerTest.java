package com.platform.chat.modules.application.handler;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.chatUser.service.ChatUserService;
import com.platform.chat.core.modules.application.entity.Application;
import com.platform.chat.core.modules.application.enums.ApplicationMessageKey;
import com.platform.chat.core.modules.application.enums.ApplicationStatus;
import com.platform.chat.core.modules.application.enums.ApplicationType;
import com.platform.chat.core.modules.application.model.ApplicationDecision;
import com.platform.chat.core.modules.application.model.DocumentNumberGenerator;
import com.platform.chat.core.modules.domainEvent.events.CharacterUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Стратегия REGISTER_PASSPORT: возвращает решение ядру (сама заявку не меняет),
 * применяет эффект через API модуля персонажа, а о факте «персонаж изменился»
 * сообщает событием в решении — публикует его ядро.
 */
@ExtendWith(MockitoExtension.class)
class RegisterPassportHandlerTest {

    private static final Long CHARACTER_ID = 7L;
    private static final String PASSPORT_ID = "PP-1111-2222";

    @Mock
    private ChatUserService chatUserService;

    @Mock
    private DocumentNumberGenerator documentNumberGenerator;

    private RegisterPassport handler;

    @BeforeEach
    void setUp() {
        handler = new RegisterPassport(chatUserService, documentNumberGenerator);
    }

    private Application application() {
        Application application = new Application();
        application.setId(1L);
        application.setCharacterId(CHARACTER_ID);
        application.setType(ApplicationType.REGISTER_PASSPORT);
        return application;
    }

    private ChatUser character() {
        ChatUser character = new ChatUser();
        character.setId(CHARACTER_ID);
        return character;
    }

    @Test
    void approvesAndReportsCharacterChanged() {
        when(chatUserService.getChatUserById(CHARACTER_ID)).thenReturn(character());
        when(documentNumberGenerator.next(ApplicationType.REGISTER_PASSPORT)).thenReturn(PASSPORT_ID);

        ApplicationDecision decision = handler.resolve(application());

        assertThat(decision.status()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(decision.messageKey()).isEqualTo(ApplicationMessageKey.PASSPORT_ISSUED);
        assertThat(decision.args()).containsExactly(PASSPORT_ID);
        assertThat(decision.events()).singleElement().isInstanceOf(CharacterUpdatedEvent.class);
        assertThat(((CharacterUpdatedEvent) decision.events().get(0)).getCharacterId())
                .isEqualTo(CHARACTER_ID);
        verify(chatUserService).issuePassport(CHARACTER_ID, PASSPORT_ID);
    }

    @Test
    void rejectsWithoutEffectsWhenPassportAlreadyIssued() {
        ChatUser character = character();
        character.setPassportId("PP-0000-0000");
        when(chatUserService.getChatUserById(CHARACTER_ID)).thenReturn(character);

        ApplicationDecision decision = handler.resolve(application());

        assertThat(decision.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(decision.messageKey()).isEqualTo(ApplicationMessageKey.PASSPORT_ALREADY_EXISTS);
        assertThat(decision.events()).isEmpty();
        verify(chatUserService, never()).issuePassport(anyLong(), anyString());
        verifyNoInteractions(documentNumberGenerator);
    }
}
