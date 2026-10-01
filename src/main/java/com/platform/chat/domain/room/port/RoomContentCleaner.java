package com.platform.chat.domain.room.port;

import java.util.List;

/**
 * Порт: удаление контента комнат, которым владеет другой слайс.
 *
 * <p>Комната — базовая сущность, но её содержимое (сообщения чата) принадлежит
 * слайсу {@code chat.engines.chatRoom}. Связи {@code Room.messages} на стороне
 * базы нет: базовый модуль комнат объявляет только контракт — <b>чистить
 * содержимое своих комнат должен тот, кто ими владеет</b>. Реализацию даёт
 * слайс чата (инверсия зависимости — как у {@code TickHandler} /
 * {@code PollResultHandler}).</p>
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
