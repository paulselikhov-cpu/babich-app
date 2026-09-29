# Модуль реакций на доменные события (подписчики)

Слайс `chat.modules.domainEvent` — «Слой 4: Доставка»: тут живут подписчики на
факты из ядра событий (`chat.core.modules.domainEvent`). Самих событий здесь нет —
их словарь часть ядра (см. `documentation/architecture/event/domain-event-module-layers.md`).

## Когда добавлять подписчика сюда

Новое последствие уже произошедшего факта: уведомление, WS-пуш, аудит-запись,
запуск другого процесса. Если эффект обязан случиться атомарно вместе с
бизнес-изменением — событие не подходит, это синхронный вызов в сервисе.

## Как добавить

1. Событие уже есть в ядре. Если нет — сначала заведи его там: значение в
   `EventType`, класс в `core/modules/domainEvent/events/`, подтип в
   `@JsonSubTypes`, собственный `@JsonCreator` и чтение чисел payload через
   `longPayload(...)`.
2. Новый `@Component` в `listeners/`. Один класс — одно семейство последствий.
3. Метод с `@ApplicationModuleListener` (AFTER_COMMIT + outbox
   `event_publication`) и параметром-событием.
4. Своя `@Transactional` — только `REQUIRES_NEW` / `NOT_SUPPORTED`. Обычную
   нельзя: подписчик вызывается после коммита, когда «текущей» транзакции нет.
5. Читай из события id, сущности догружай сам.
6. Напиши тест на эффект (запись в БД / WS-пуш), а не на факт вызова метода.

## Границы

`allowedDependencies`: `chat.base.chatUser`, `chat.core.modules.domainEvent`,
`chat.core.modules.notification`, `chat.core.modules.transactionLog`.
Обратных импортов нет — ни один публикатор событий этот слайс не импортирует,
поэтому цикла «ядро ↔ реакции» не возникает.
