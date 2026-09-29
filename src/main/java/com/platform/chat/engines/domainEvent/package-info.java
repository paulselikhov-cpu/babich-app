/**
 * <b>Ядро событийной шины</b>: конверт события ({@code DomainEvent}), словарь
 * фактов ({@code EventType}, {@code ScopeType}, {@code events/*}) и порт
 * публикации ({@code DomainEventPublisher}).
 *
 * <p>Конкретные события-факты живут здесь, а не в фич-слайсе подписчиков,
 * потому что их публикуют <b>ядра процессов</b> ({@code core.modules.application},
 * {@code core.modules.poll}) и фич-модули ({@code coin}, {@code governorElection}):
 * если бы события лежали в фиче, каждое ядро тянуло бы фич-слайс — это
 * направленный цикл между ядром и исполнителями (тот же дефект, что был с
 * {@code ApplicationType}). Сейчас зависимость строго однонаправленная:
 * {@code chat.modules.domainEvent → chat.core.modules.domainEvent}.
 *
 * <p>Ядро не знает ни про подписчиков, ни про {@code ChatUser}, ни про web —
 * только про факты и их доставку.
 *
 * <p><b>allowedDependencies</b> — управляет тем, куда модуль может обращаться сам.
 * Для <code>@NamedInterface</code> вид: <code>allowedDependencies = {"auth::userRepository"}</code>.
 *
 * <p><b>type</b> — управляет тем, что модуль отдаёт наружу.
 * Если <code>CLOSED</code> — ничего не отдаёт, и тогда, чтобы что-то отдать,
 * нужно прописывать классу <code>@NamedInterface("userRepository")</code>.
 */
@ApplicationModule(
        allowedDependencies = {
                "chat.core.modules.transactionLog"
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.domainEvent;

import org.springframework.modulith.ApplicationModule;