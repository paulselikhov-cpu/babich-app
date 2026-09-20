@ApplicationModule(
        allowedDependencies = {
                "chat.base.chatUser",
                "chat.base.room",
                "chat.base.district",
                "chat.core.modules.message",
                "chat.core.modules.onlineSession",
                "chat.modules.coin",
                "chat.modules.gang",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.base.location;

import org.springframework.modulith.ApplicationModule;