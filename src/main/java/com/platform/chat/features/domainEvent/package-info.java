/**
 * <b>Реакции на доменные события</b> (Слой 4 «Доставка»): подписчики
 * ({@code @ApplicationModuleListener}), которые превращают факт в эффект —
 * уведомление жителю/заявителю, WS-пуш об изменении персонажа, аудит-запись
 * в {@code TransactionLog}.
 *
 * <p>Модуль знает только про ядро событий ({@code chat.engines.domainEvent})
 * и про модули, чьи эффекты он применяет. Про публикаторов он не знает ничего:
 * ни один сервис-издатель не импортирует этот слайс — подписка идёт по факту
 * события. Событий-классов здесь нет: они часть словаря ядра.
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
                "chat.domain.chatUser",
                "chat.engines.domainEvent",
                "chat.engines.transactionLog",
                "chat.engines.notification",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.features.domainEvent;

import org.springframework.modulith.ApplicationModule;