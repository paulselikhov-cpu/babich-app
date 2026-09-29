package com.platform.chat.domain.chatUser.service;

import com.platform.auth.entity.User;
import com.platform.auth.repository.UserRepository;
import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.chatUser.enums.ChatRole;
import com.platform.chat.domain.chatUser.repository.ChatUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Проверки владения идентификатором для ручек, которые получают
 * {@code characterId}/{@code userId} из запроса, а не из {@code Principal}.
 *
 * <p>Без этих проверок любой залогиненный игрок мог читать и менять состояние
 * чужого персонажа (IDOR). Тест фиксирует разрешённые формы id — фронт исторически
 * передаёт и {@code ChatUser.id}, и {@code User.id} — и запрет на чужие id.</p>
 *
 * <p>Чистый unit-тест: без Spring-контекста и без БД.</p>
 */
@ExtendWith(MockitoExtension.class)
class ChatUserServiceTest {

    @Mock
    private ChatUserRepository chatUserRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonLevelService personLevelService;

    @InjectMocks
    private ChatUserService chatUserService;

    /** Персонаж id=10, аккаунт id=1, username «vasya». */
    private ChatUser ownCharacter;

    /** Второй персонаж того же аккаунта (другой район). */
    private ChatUser ownSecondCharacter;

    /** Персонаж id=20 чужого аккаунта id=2. */
    private ChatUser foreignCharacter;

    @BeforeEach
    void setUp() {
        ownCharacter = character(10L, 1L, "vasya");
        ownSecondCharacter = character(11L, 1L, "vasya");
        foreignCharacter = character(20L, 2L, "petya");
    }

    @Test
    void allowsOwnCharacterId() {
        when(chatUserRepository.findByUsername("vasya")).thenReturn(Optional.of(ownCharacter));

        assertThatCode(() -> chatUserService.assertCharacterOwnedBy("vasya", 10L))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsOwnAccountId() {
        // Фронт местами передаёт User.id вместо ChatUser.id — внутри своего аккаунта это допустимо.
        when(chatUserRepository.findByUsername("vasya")).thenReturn(Optional.of(ownCharacter));

        assertThatCode(() -> chatUserService.assertCharacterOwnedBy("vasya", 1L))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsSecondCharacterOfSameAccount() {
        when(chatUserRepository.findByUsername("vasya")).thenReturn(Optional.of(ownCharacter));
        when(chatUserRepository.findById(11L)).thenReturn(Optional.of(ownSecondCharacter));

        assertThatCode(() -> chatUserService.assertCharacterOwnedBy("vasya", 11L))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsForeignCharacter() {
        when(chatUserRepository.findByUsername("vasya")).thenReturn(Optional.of(ownCharacter));
        when(chatUserRepository.findById(20L)).thenReturn(Optional.of(foreignCharacter));

        assertThatThrownBy(() -> chatUserService.assertCharacterOwnedBy("vasya", 20L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("чужому персонажу");
    }

    @Test
    void rejectsUnknownCharacter() {
        when(chatUserRepository.findByUsername("vasya")).thenReturn(Optional.of(ownCharacter));
        when(chatUserRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatUserService.assertCharacterOwnedBy("vasya", 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsNullCharacterId() {
        assertThatThrownBy(() -> chatUserService.assertCharacterOwnedBy("vasya", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("не указан");
    }

    @Test
    void rejectsActorWithoutCharacter() {
        when(chatUserRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatUserService.assertCharacterOwnedBy("ghost", 10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void allowsOwnAccountOnOnboarding() {
        // Онбординг: персонажа ещё нет, но userId — свой.
        when(userRepository.findByUsername("vasya")).thenReturn(Optional.of(account(1L, "vasya")));

        assertThatCode(() -> chatUserService.assertAccountIsSelfOrAdmin("vasya", 1L))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsForeignAccountForRegularUser() {
        when(userRepository.findByUsername("vasya")).thenReturn(Optional.of(account(1L, "vasya")));
        when(chatUserRepository.findByUsername("vasya")).thenReturn(Optional.of(ownCharacter));

        assertThatThrownBy(() -> chatUserService.assertAccountIsSelfOrAdmin("vasya", 2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("чужому аккаунту");
    }

    @Test
    void allowsForeignAccountForAdmin() {
        ChatUser admin = character(30L, 1L, "admin");
        admin.setRole(ChatRole.ADMIN);

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(account(1L, "admin")));
        when(chatUserRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatCode(() -> chatUserService.assertAccountIsSelfOrAdmin("admin", 2L))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullAccountId() {
        assertThatThrownBy(() -> chatUserService.assertAccountIsSelfOrAdmin("vasya", null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("не указан");
    }

    private ChatUser character(Long id, Long userId, String username) {
        ChatUser character = new ChatUser();
        character.setId(id);
        character.setUserId(userId);
        character.setUsername(username);
        return character;
    }

    private User account(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }
}
