/**
 * <b>allowedDependencies</b> — управляет тем, куда модуль может обращаться сам.
 * Для <code>@NamedInterface</code> вид: <code>allowedDependencies = {"auth::userRepository"}</code>.
 *
 * <p><b>type</b> — управляет тем, что модуль отдаёт наружу.
 * Если <code>CLOSED</code> — ничего не отдаёт, и тогда, чтобы что-то отдать,
 * нужно прописывать классу <code>@NamedInterface("userRepository")</code>.
 */
@ApplicationModule(
        allowedDependencies = {
                "auth::JwtService",
                "auth::entityUser",
                "auth::userRepository",
                "chat.base.chatUser",
                "chat.core.modules.application",
                "chat.modules.work"
        },
        type = ApplicationModule.Type.CLOSED
)
package com.platform.chat.modules.application;

import org.springframework.modulith.ApplicationModule;