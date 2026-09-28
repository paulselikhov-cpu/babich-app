@ApplicationModule(
        allowedDependencies = {
                "auth::JwtService",
                "chat.base.chatUser",
                "chat.core.modules.chatRoom"
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.webSocket;

import org.springframework.modulith.ApplicationModule;