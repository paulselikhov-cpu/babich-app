/**
 * <b>Ядро процесса гражданской заявки</b>: жизненный цикл
 * (PENDING → APPROVED/REJECTED), доменный словарь типов, реестр стратегий,
 * чтения и публикация процессных событий.
 *
 * <p>Модуль НЕ знает ни про {@code chat.base.chatUser}, ни про {@code auth},
 * ни про web: конкретные последствия одобрения (выдача паспорта/лицензии)
 * реализуются стратегиями в {@code chat.modules.application}, которые зависят
 * от ядра, но не наоборот. Это убирает направленный цикл между ядром процесса
 * и исполнителями заявок.</p>
 */
@ApplicationModule(
        allowedDependencies = {
                "chat.core.modules.domainEvent",
                "chat.modules.domainEvent" },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.application;

import org.springframework.modulith.ApplicationModule;
