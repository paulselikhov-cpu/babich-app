package com.platform.chat.domain.chatUser.service;

import com.platform.chat.domain.chatUser.entity.ChatUser;
import com.platform.chat.domain.chatUser.enums.PersonLevel;
import org.springframework.stereotype.Service;

@Service
public class PersonLevelService {

    /**
     * Получить уровень персонажа.
     */
    public PersonLevel getLevel(ChatUser character) {
        return PersonLevel.fromXp(character.getXpUser());
    }

    public long getLevelNumber(ChatUser character) {
        return getLevel(character).getLevelNumber();
    }

    /**
     * Проверить, достиг ли персонаж указанного уровня (включительно).
     */
    public boolean hasMinLevel(ChatUser character, PersonLevel required) {
        return getLevel(character).ordinal() >= required.ordinal();
    }

    /**
     * Порог XP следующего уровня (для прогресс-бара на UI).
     * Если персонаж на максимальном уровне — возвращает порог текущего уровня
     * (прогресс считается заполненным).
     */
    public long nextLevelXpThreshold(ChatUser character) {
        PersonLevel current = getLevel(character);
        PersonLevel[] levels = PersonLevel.values();
        int nextIndex = current.ordinal() + 1;
        if (nextIndex >= levels.length) {
            return current.getXpThreshold();
        }
        return levels[nextIndex].getXpThreshold();
    }

    /**
     * Получить XP, необходимый для следующего уровня.
     * Возвращает 0, если персонаж уже на максимальном уровне.
     */
    public long xpToNextLevel(ChatUser character) {
        PersonLevel current = getLevel(character);
        PersonLevel[] levels = PersonLevel.values();
        int nextIndex = current.ordinal() + 1;
        if (nextIndex >= levels.length) {
            return 0;
        }
        return levels[nextIndex].getXpThreshold() - character.getXpUser();
    }
}