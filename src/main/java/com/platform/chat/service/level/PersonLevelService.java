package com.platform.chat.service.level;

import com.platform.chat.entity.ChatUser;
import com.platform.chat.enums.PersonLevel;
import org.springframework.stereotype.Service;

/**
 * Слой 1 (Интерфейсы). Сервис уровней персонажа.
 * Вычисляет PersonLevel из xpUser и проверяет пороговые условия.
 */
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