package com.platform.chat.base.room.port;

import java.util.List;

/**
 * Порт: удаление контента комнат, которым владеет другой слайс.
 *
 * <p>Комната — базовая сущность, но её содержимое (сообщения чата) принадлежит
 * слайсу {@code chat.core.modules.chatRoom}. Раньше это выражалось обратной
 * связью {@code Room.messages} с {@code cascade = ALL} — то есть базовый модуль
 * комнат импортировал entity чата (и держал ленивую коллекцию на тысячи строк),
 * хотя сама связь нигде не читалась. Теперь связь убрана, а база объявляет
 * только контракт: <b>чистить содержимое своих комнат должен тот, кто им
 * владеет</b>. Реализацию даёт слайс чата (инверсия зависимости — как у
 * {@code TickHandler} / {@code PollResultHandler}).</p>
 *
 * <p>Вызывать синхронно, в той же транзакции и <b>до</b> удаления строк
 * {@code rooms}: FK {@code messages.room_id → rooms.id} иначе не даст удалить
 * комнату. Порядок обеспечивает {@code LocationService.deleteLocation}.</p>
 */
public interface RoomContentCleaner {

    /**
     * Удалить весь контент переданных комнат.
     * Пустой список — no-op.
     */
    void cleanRooms(List<Long> roomIds);
}
