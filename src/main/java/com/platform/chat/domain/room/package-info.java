@ApplicationModule(
        allowedDependencies = {
                "chat.domain.location",
                "chat.domain.chatUser",
        },
        type = ApplicationModule.Type.OPEN
)
package com.platform.chat.domain.room;

import org.springframework.modulith.ApplicationModule;