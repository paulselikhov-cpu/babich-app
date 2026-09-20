@ApplicationModule(
        allowedDependencies = {
                "chat.core.modules.onlineSession",
                "chat.base.chatUser",
                "chat.base.room",
                "chat.base.location"
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.core.modules.message;

import org.springframework.modulith.ApplicationModule;