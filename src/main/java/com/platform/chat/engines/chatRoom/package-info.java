/**
 * <b>Чат комнаты</b>: всё, что происходит с персонажем внутри комнаты —
 * сообщения, непрочитанное и присутствие (кто сейчас онлайн и в какой комнате).
 *
 *
 * <p>Что здесь: entity {@code Message} и {@code OnlineSession} (таблица
 * {@code character_presence}), репозитории, {@code MessageService} (история,
 * отправка, soft delete), {@code UnreadService} (счётчики непрочитанного,
 * отметка чтения, WS-пуш бейджей), {@code OnlineSessionService} (вход/выход/
 * heartbeat/очистка протухших сессий по {@code TickHandler}, WS-присутствие),
 * контроллеры REST ({@code /api/messages}, состояние комнат в
 * {@code /api/location-users}) и DTO.</p>
 *
 * <p>Что осознанно НЕ здесь: {@code Room}/{@code RoomCode}/{@code TemplateRoom} —
 * базовая модель комнаты; {@code RoomReadStatus} — тоже остаётся в
 * {@code chat.base.room}, потому что каскадная чистка «прочитано» при удалении
 * комнат должна идти в одной транзакции с их удалением (FK
 * {@code room_read_status.room_id → rooms.id}); сервисы слайса работают с этой
 * сущностью как с общей базовой моделью. Сообщения же — данные слайса, поэтому
 * их чистку база запрашивает через свой порт
 * {@code chat.base.room.port.RoomContentPurger}.</p>
 *
 * <p>Внешние контракты (не менять без необходимости): STOMP-маппинги
 * {@code /chat.history.send}, {@code /chat.join}, {@code /chat.typing},
 * {@code /chat.leave}, {@code /chat.read}, {@code /presence.heartbeat};
 * топики {@code /topic/room.{id}}, {@code /topic/typing.{id}},
 * {@code /topic/location.{id}.presence}, {@code /topic/locations.presence-counts},
 * {@code /user/queue/unread-updates}; REST {@code /api/messages/**} и
 * presence/unread-ручки под {@code /api/location-users}.</p>
 *
 * <p><b>allowedDependencies</b> — управляет тем, куда модуль может обращаться сам.
 * Для <code>@NamedInterface</code> вид: <code>allowedDependencies = {"auth::userRepository"}</code>.</p>
 *
 * <p><b>type</b> — управляет тем, что модуль отдаёт наружу.
 * Если <code>CLOSED</code> — ничего не отдаёт, и тогда, чтобы что-то отдать,
 * нужно прописывать классу <code>@NamedInterface("userRepository")</code>.</p>
 */
@ApplicationModule(
        allowedDependencies = {
                "chat.base.chatUser",
                "chat.base.room",
                "chat.base.location",
                "chat.core.modules.scheduledCheck",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.chatRoom;

import org.springframework.modulith.ApplicationModule;
