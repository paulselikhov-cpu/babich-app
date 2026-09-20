@ApplicationModule(
        allowedDependencies = {
                "chat.base.location",
                "chat.base.chatUser",
                "chat.core.modules.message",
                "chat.core.modules.onlineSession",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.base.room;

import org.springframework.modulith.ApplicationModule;