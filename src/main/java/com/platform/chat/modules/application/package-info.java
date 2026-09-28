/**
 * <b>Исполнители гражданских заявок</b> (стратегии по {@code ApplicationType})
 * и REST мэрии. Модуль знает про конкретные последствия одобрения — правки
 * {@code ChatUser} (паспорт, лицензия), поэтому зависит от {@code chat.base.chatUser}.
 * Ядро процесса ({@code chat.core.modules.application}) про этот модуль не знает:
 * зависимость строго однонаправленная (feature → core).
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
                "auth::userService",
                "chat.base.chatUser",
                "chat.core.modules.application",
                "chat.core.modules.application::ApplicationType",
                "chat.core.modules.domainEvent"
        },
        type = ApplicationModule.Type.CLOSED
)
package com.platform.chat.modules.application;

import org.springframework.modulith.ApplicationModule;
